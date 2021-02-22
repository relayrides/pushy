/*
 * Copyright (c) 2020 Jon Chambers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.eatthepath.pushy.apns;

import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.proxy.ProxyHandlerFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.resolver.AddressResolverGroup;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.resolver.dns.DefaultDnsServerAddressStreamProvider;
import io.netty.resolver.dns.RoundRobinDnsAddressResolverGroup;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCounted;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.cert.CertPathBuilderException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An APNs channel factory creates new channels connected to an APNs server. Channels constructed by this factory are
 * intended for use in an {@link ApnsChannelPool}.
 */
class ApnsChannelFactory implements PooledObjectFactory<Channel>, Closeable {

    private final SslContext sslContext;
    private final AtomicBoolean hasReleasedSslContext = new AtomicBoolean(false);

    private final AddressResolverGroup<? extends SocketAddress> addressResolverGroup;

    private final Bootstrap bootstrapTemplate;

    private final AtomicLong currentDelaySeconds = new AtomicLong(0);

    private static final long MIN_CONNECT_DELAY_SECONDS = 1;
    private static final long MAX_CONNECT_DELAY_SECONDS = 60;

    static final AttributeKey<Promise<Channel>> CHANNEL_READY_PROMISE_ATTRIBUTE_KEY =
            AttributeKey.valueOf(ApnsChannelFactory.class, "channelReadyPromise");

    private static final Logger log = LoggerFactory.getLogger(ApnsChannelFactory.class);

    ApnsChannelFactory(final SslContext sslContext, final ApnsSigningKey signingKey, final Duration tokenExpiration,
                       final ProxyHandlerFactory proxyHandlerFactory, final Duration connectTimeout,
                       final Duration idlePingInterval, final Duration gracefulShutdownTimeout,
                       final Http2FrameLogger frameLogger, final InetSocketAddress apnsServerAddress,
                       final EventLoopGroup eventLoopGroup) {

        this.sslContext = sslContext;

        if (this.sslContext instanceof ReferenceCounted) {
            ((ReferenceCounted) this.sslContext).retain();
        }

        this.addressResolverGroup = proxyHandlerFactory == null ?
                new RoundRobinDnsAddressResolverGroup(ClientChannelClassUtil.getDatagramChannelClass(eventLoopGroup),
                        DefaultDnsServerAddressStreamProvider.INSTANCE) : NoopAddressResolverGroup.INSTANCE;

        this.bootstrapTemplate = new Bootstrap();
        this.bootstrapTemplate.group(eventLoopGroup);
        this.bootstrapTemplate.option(ChannelOption.TCP_NODELAY, true);
        this.bootstrapTemplate.remoteAddress(apnsServerAddress);
        this.bootstrapTemplate.resolver(this.addressResolverGroup);

        if (connectTimeout != null) {
            this.bootstrapTemplate.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis());
        }

        this.bootstrapTemplate.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(final SocketChannel channel) {
                final String authority = apnsServerAddress.getHostName();
                final SslHandler sslHandler = sslContext.newHandler(channel.alloc(), authority, apnsServerAddress.getPort());

                final ApnsClientHandler apnsClientHandler;
                {
                    final ApnsClientHandler.ApnsClientHandlerBuilder clientHandlerBuilder;

                    if (signingKey != null) {
                        clientHandlerBuilder = new TokenAuthenticationApnsClientHandler.TokenAuthenticationApnsClientHandlerBuilder()
                                .signingKey(signingKey)
                                .tokenExpiration(tokenExpiration)
                                .authority(authority)
                                .idlePingInterval(idlePingInterval);
                    } else {
                        clientHandlerBuilder = new ApnsClientHandler.ApnsClientHandlerBuilder()
                                .authority(authority)
                                .idlePingInterval(idlePingInterval);
                    }

                    if (frameLogger != null) {
                        clientHandlerBuilder.frameLogger(frameLogger);
                    }

                    apnsClientHandler = clientHandlerBuilder.build();

                    if (gracefulShutdownTimeout != null) {
                        apnsClientHandler.gracefulShutdownTimeoutMillis(gracefulShutdownTimeout.toMillis());
                    }
                }

                final ChannelPipeline pipeline = channel.pipeline();

                if (proxyHandlerFactory != null) {
                    pipeline.addFirst(proxyHandlerFactory.createProxyHandler());
                }

                pipeline.addLast(sslHandler);
                pipeline.addLast(new FlushConsolidationHandler(FlushConsolidationHandler.DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES, true));
                pipeline.addLast(new IdleStateHandler(idlePingInterval.toMillis(), 0, 0, TimeUnit.MILLISECONDS));
                pipeline.addLast(apnsClientHandler);
            }
        });
    }

    /**
     * Creates and connects a new channel. The initial connection attempt may be delayed to accommodate exponential
     * back-off requirements.
     *
     * @param channelReadyPromise the promise to be notified when a channel has been created and connected to the APNs
     * server
     *
     * @return a future that will be notified once a channel has been created and connected to the APNs server
     */
    @Override
    public Future<Channel> create(final Promise<Channel> channelReadyPromise) {
        final long delay = this.currentDelaySeconds.get();

        channelReadyPromise.addListener(future -> {
            final long updatedDelay = future.isSuccess() ? 0 :
                    Math.max(Math.min(delay * 2, MAX_CONNECT_DELAY_SECONDS), MIN_CONNECT_DELAY_SECONDS);

            ApnsChannelFactory.this.currentDelaySeconds.compareAndSet(delay, updatedDelay);

            if (isCertificatePathException(future.cause())) {
                log.warn("Unable to build a certificate path to the APNs server's certificate. This usually means that your system's default trust store does not include the root CA for the APNs server. For more information and possible solutions, please see TODO.");
            }
        });


        this.bootstrapTemplate.config().group().schedule(() -> {
            final Bootstrap bootstrap = ApnsChannelFactory.this.bootstrapTemplate.clone()
                    .channelFactory(new AugmentingReflectiveChannelFactory<>(
                            ClientChannelClassUtil.getSocketChannelClass(ApnsChannelFactory.this.bootstrapTemplate.config().group()),
                            CHANNEL_READY_PROMISE_ATTRIBUTE_KEY, channelReadyPromise));

            final ChannelFuture connectFuture = bootstrap.connect();

            connectFuture.addListener(future -> {
                if (!future.isSuccess()) {
                    channelReadyPromise.tryFailure(future.cause());
                }
            });
        }, delay, TimeUnit.SECONDS);

        return channelReadyPromise;
    }

    /**
     * Tests whether the given {@code Throwable} is or is caused by a {@link CertPathBuilderException}.
     *
     * @param throwable the throwable to test for a {@code CertPathBuilderExceptionCause}; may be {@code null}
     *
     * @return {@code true} if the given throwable is or is caused by a {@code CertPathBuilderException} or
     * {@code false} otherwise
     */
    static boolean isCertificatePathException(final Throwable throwable) {
        return throwable != null && (throwable instanceof CertPathBuilderException || isCertificatePathException(throwable.getCause()));
    }

    /**
     * Destroys a channel by closing it.
     *
     * @param channel the channel to destroy
     * @param promise the promise to notify when the channel has been destroyed
     *
     * @return a future that will be notified when the channel has been destroyed
     */
    @Override
    public Future<Void> destroy(final Channel channel, final Promise<Void> promise) {
        channel.close().addListener(new PromiseNotifier<>(promise));
        return promise;
    }

    @Override
    public void close() {
        try {
            this.addressResolverGroup.close();
        } finally {
            if (this.sslContext instanceof ReferenceCounted) {
                if (this.hasReleasedSslContext.compareAndSet(false, true)) {
                    ((ReferenceCounted) this.sslContext).release();
                }
            }
        }
    }
}
