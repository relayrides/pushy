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
import com.eatthepath.pushy.apns.auth.ApnsVerificationKey;
import com.eatthepath.pushy.apns.auth.KeyPairUtil;
import com.eatthepath.pushy.apns.server.MockApnsServer;
import com.eatthepath.pushy.apns.server.MockApnsServerBuilder;
import com.eatthepath.pushy.apns.server.MockApnsServerListener;
import com.eatthepath.pushy.apns.server.PushNotificationHandlerFactory;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.*;

@Timeout(10)
public class AbstractClientServerTest {

    protected static NioEventLoopGroup CLIENT_EVENT_LOOP_GROUP;
    protected static NioEventLoopGroup SERVER_EVENT_LOOP_GROUP;

    protected static final String CA_CERTIFICATE_FILENAME = "/ca.pem";
    protected static final String SERVER_CERTIFICATES_FILENAME = "/server-certs.pem";
    protected static final String SERVER_KEY_FILENAME = "/server-key.pem";

    protected static final String MULTI_TOPIC_CLIENT_KEYSTORE_FILENAME = "/multi-topic-client.p12";
    protected static final String KEYSTORE_PASSWORD = "pushy-test";

    protected static final String HOST = "localhost";
    protected static final int PORT = 8443;

    protected static final String TEAM_ID = "team-id";
    protected static final String KEY_ID = "key-id";
    protected static final String TOPIC = "com.relayrides.pushy";
    protected static final String DEVICE_TOKEN = generateRandomDeviceToken();
    protected static final String PAYLOAD = generateRandomPayload();

    protected static final Map<String, Set<String>> DEVICE_TOKENS_BY_TOPIC =
            Collections.singletonMap(TOPIC, Collections.singleton(DEVICE_TOKEN));

    protected static final Map<String, Instant> EXPIRATION_TIMESTAMPS_BY_DEVICE_TOKEN = Collections.emptyMap();

    protected static final int TOKEN_LENGTH = 32; // bytes

    protected ApnsSigningKey signingKey;
    protected Map<String, ApnsVerificationKey> verificationKeysByKeyId;
    protected Map<ApnsVerificationKey, Set<String>> topicsByVerificationKey;

    @BeforeAll
    public static void setUpBeforeClass() {
        CLIENT_EVENT_LOOP_GROUP = new NioEventLoopGroup(2);
        SERVER_EVENT_LOOP_GROUP = new NioEventLoopGroup(2);
    }

    @BeforeEach
    public void setUp() throws Exception {
        final KeyPair keyPair = KeyPairUtil.generateKeyPair();

        this.signingKey = new ApnsSigningKey(KEY_ID, TEAM_ID, (ECPrivateKey) keyPair.getPrivate());
        final ApnsVerificationKey verificationKey =
                new ApnsVerificationKey(KEY_ID, TEAM_ID, (ECPublicKey) keyPair.getPublic());

        this.verificationKeysByKeyId = Collections.singletonMap(KEY_ID, verificationKey);
        this.topicsByVerificationKey = Collections.singletonMap(verificationKey, Collections.singleton(TOPIC));
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        final PromiseCombiner combiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        combiner.addAll(CLIENT_EVENT_LOOP_GROUP.shutdownGracefully(), SERVER_EVENT_LOOP_GROUP.shutdownGracefully());

        final Promise<Void> shutdownPromise = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
        combiner.finish(shutdownPromise);
        shutdownPromise.await();
    }

    protected ApnsClient buildTlsAuthenticationClient() throws IOException, CertificateException {
        return this.buildTlsAuthenticationClient(null);
    }

    protected ApnsClient buildTlsAuthenticationClient(final ApnsClientMetricsListener metricsListener) throws IOException, CertificateException {
        try (final InputStream p12InputStream = getClass().getResourceAsStream(MULTI_TOPIC_CLIENT_KEYSTORE_FILENAME)) {
            return new ApnsClientBuilder()
                    .setApnsServer(HOST, PORT)
                    .setClientCredentials(p12InputStream, KEYSTORE_PASSWORD)
                    .setTrustedServerCertificateChain(getClass().getResourceAsStream(CA_CERTIFICATE_FILENAME))
                    .setEventLoopGroup(CLIENT_EVENT_LOOP_GROUP)
                    .setMetricsListener(metricsListener)
                    .build();
        }
    }

    protected ApnsClient buildTokenAuthenticationClient() throws IOException, CertificateException {
        return this.buildTokenAuthenticationClient(null);
    }

    protected ApnsClient buildTokenAuthenticationClient(final ApnsClientMetricsListener metricsListener) throws IOException, CertificateException {
        return new ApnsClientBuilder()
                .setApnsServer(HOST, PORT)
                .setTrustedServerCertificateChain(getClass().getResourceAsStream(CA_CERTIFICATE_FILENAME))
                .setSigningKey(this.signingKey)
                .setEventLoopGroup(CLIENT_EVENT_LOOP_GROUP)
                .setMetricsListener(metricsListener)
                .build();
    }

    protected MockApnsServer buildServer(final PushNotificationHandlerFactory handlerFactory) throws SSLException {
        return this.buildServer(handlerFactory, null);
    }

    protected MockApnsServer buildServer(final PushNotificationHandlerFactory handlerFactory, final MockApnsServerListener listener) throws SSLException {
        return new MockApnsServerBuilder()
                .setServerCredentials(getClass().getResourceAsStream(SERVER_CERTIFICATES_FILENAME), getClass().getResourceAsStream(SERVER_KEY_FILENAME), null)
                .setTrustedClientCertificateChain(getClass().getResourceAsStream(CA_CERTIFICATE_FILENAME))
                .setEventLoopGroup(SERVER_EVENT_LOOP_GROUP)
                .setHandlerFactory(handlerFactory)
                .setListener(listener)
                .build();
    }

    protected static String generateRandomDeviceToken() {
        final byte[] tokenBytes = new byte[TOKEN_LENGTH];
        new Random().nextBytes(tokenBytes);

        final StringBuilder builder = new StringBuilder(TOKEN_LENGTH * 2);

        for (final byte b : tokenBytes) {
            builder.append(String.format("%02x", b));
        }

        return builder.toString();
    }

    protected static String generateRandomPayload() {
        final ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setAlertBody(UUID.randomUUID().toString());

        return payloadBuilder.build();
    }
}
