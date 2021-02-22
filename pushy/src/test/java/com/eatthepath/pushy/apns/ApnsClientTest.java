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

import com.eatthepath.pushy.apns.server.*;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ApnsClientTest extends AbstractClientServerTest {

    private static class TestClientMetricsListener implements ApnsClientMetricsListener {

        private final List<Long> writeFailures = new ArrayList<>();
        private final List<Long> sentNotifications = new ArrayList<>();
        private final List<Long> acceptedNotifications = new ArrayList<>();
        private final List<Long> rejectedNotifications = new ArrayList<>();

        private final AtomicInteger connectionsAdded = new AtomicInteger(0);
        private final AtomicInteger connectionsRemoved = new AtomicInteger(0);
        private final AtomicInteger failedConnectionAttempts = new AtomicInteger(0);

        @Override
        public void handleWriteFailure(final ApnsClient apnsClient, final long notificationId) {
            synchronized (this.writeFailures) {
                this.writeFailures.add(notificationId);
                this.writeFailures.notifyAll();
            }
        }

        @Override
        public void handleNotificationSent(final ApnsClient apnsClient, final long notificationId) {
            this.sentNotifications.add(notificationId);
        }

        @Override
        public void handleNotificationAccepted(final ApnsClient apnsClient, final long notificationId) {
            synchronized (this.acceptedNotifications) {
                this.acceptedNotifications.add(notificationId);
                this.acceptedNotifications.notifyAll();
            }
        }

        @Override
        public void handleNotificationRejected(final ApnsClient apnsClient, final long notificationId) {
            synchronized (this.rejectedNotifications) {
                this.rejectedNotifications.add(notificationId);
                this.rejectedNotifications.notifyAll();
            }
        }

        @Override
        public void handleConnectionAdded(final ApnsClient apnsClient) {
            synchronized (this.connectionsAdded) {
                this.connectionsAdded.incrementAndGet();
                this.connectionsAdded.notifyAll();
            }
        }

        @Override
        public void handleConnectionRemoved(final ApnsClient apnsClient) {
            synchronized (this.connectionsRemoved) {
                this.connectionsRemoved.incrementAndGet();
                this.connectionsRemoved.notifyAll();
            }
        }

        @Override
        public void handleConnectionCreationFailed(final ApnsClient apnsClient) {
            synchronized (this.failedConnectionAttempts) {
                this.failedConnectionAttempts.incrementAndGet();
                this.failedConnectionAttempts.notifyAll();
            }
        }

        void waitForNonZeroWriteFailures() throws InterruptedException {
            synchronized (this.writeFailures) {
                while (this.writeFailures.isEmpty()) {
                    this.writeFailures.wait();
                }
            }
        }

        void waitForNonZeroAcceptedNotifications() throws InterruptedException {
            synchronized (this.acceptedNotifications) {
                while (this.acceptedNotifications.isEmpty()) {
                    this.acceptedNotifications.wait();
                }
            }
        }

        void waitForNonZeroRejectedNotifications() throws InterruptedException {
            synchronized (this.rejectedNotifications) {
                while (this.rejectedNotifications.isEmpty()) {
                    this.rejectedNotifications.wait();
                }
            }
        }

        void waitForNonZeroFailedConnections() throws InterruptedException {
            synchronized (this.failedConnectionAttempts) {
                while (this.failedConnectionAttempts.get() == 0) {
                    this.failedConnectionAttempts.wait();
                }
            }
        }

        List<Long> getWriteFailures() {
            return this.writeFailures;
        }

        List<Long> getSentNotifications() {
            return this.sentNotifications;
        }

        List<Long> getAcceptedNotifications() {
            return this.acceptedNotifications;
        }

        List<Long> getRejectedNotifications() {
            return this.rejectedNotifications;
        }

        AtomicInteger getConnectionsAdded() {
            return this.connectionsAdded;
        }

        AtomicInteger getConnectionsRemoved() {
            return this.connectionsRemoved;
        }

        AtomicInteger getFailedConnectionAttempts() {
            return this.failedConnectionAttempts;
        }
    }

    private static class TestParsingServerHandler extends ParsingMockApnsServerListenerAdapter {

        private final List<ApnsPushNotification> acceptedNotifications = new ArrayList<>();
        private final List<ApnsPushNotification> rejectedNotifications = new ArrayList<>();

        @Override
        public void handlePushNotificationAccepted(final ApnsPushNotification pushNotification) {
            synchronized (this.acceptedNotifications) {
                this.acceptedNotifications.add(pushNotification);
                this.acceptedNotifications.notifyAll();
            }
        }

        @Override
        public void handlePushNotificationRejected(final ApnsPushNotification pushNotification, final RejectionReason rejectionReason, final Instant deviceTokenExpirationTimestamp) {
            synchronized (this.rejectedNotifications) {
                this.rejectedNotifications.add(pushNotification);
                this.rejectedNotifications.notifyAll();
            }
        }

        void waitForNonZeroAcceptedNotifications() throws InterruptedException {
            synchronized (this.acceptedNotifications) {
                while (this.acceptedNotifications.isEmpty()) {
                    this.acceptedNotifications.wait();
                }
            }
        }
    }

    @Test
    void testApnsClientWithManagedEventLoopGroup() throws Exception {
        final ApnsClient managedGroupClient = new ApnsClientBuilder()
                .setApnsServer(HOST, PORT)
                .setSigningKey(this.signingKey)
                .build();

        assertDoesNotThrow(() -> managedGroupClient.close().get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testSendNotificationToUntrustedServer(final boolean useTokenAuthentication) throws Exception {
        final ApnsClient cautiousClient;

        if (useTokenAuthentication) {
            cautiousClient = new ApnsClientBuilder()
                    .setApnsServer(HOST, PORT)
                    .setSigningKey(this.signingKey)
                    .setEventLoopGroup(CLIENT_EVENT_LOOP_GROUP)
                    .build();
        } else {
            try (final InputStream p12InputStream = getClass().getResourceAsStream(MULTI_TOPIC_CLIENT_KEYSTORE_FILENAME)) {
                cautiousClient = new ApnsClientBuilder()
                        .setApnsServer(HOST, PORT)
                        .setClientCredentials(p12InputStream, KEYSTORE_PASSWORD)
                        .setEventLoopGroup(CLIENT_EVENT_LOOP_GROUP)
                        .build();
            }
        }

        final MockApnsServer server = this.buildServer(new AcceptAllPushNotificationHandlerFactory());

        try {
            server.start(PORT).get();

            final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendFuture =
                    cautiousClient.sendNotification(new SimpleApnsPushNotification(DEVICE_TOKEN, TOPIC, PAYLOAD));

            final Throwable cause = assertThrows(ExecutionException.class, sendFuture::get,
                    "Clients must not connect to untrusted servers.");

            assertTrue(ApnsChannelFactory.isCertificatePathException(cause),
                    "Clients should refuse to connect to untrusted servers due to a certificate path exception.");
        } finally {
            cautiousClient.close().get();
            server.shutdown().get();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testRepeatedClose(final boolean useTokenAuthentication) throws Exception {
        final ApnsClient client = useTokenAuthentication ?
                this.buildTokenAuthenticationClient() : this.buildTlsAuthenticationClient();

        try {
            assertDoesNotThrow(() -> client.close().get(),
                    "Client should close successfully under normal circumstances.");

            assertDoesNotThrow(() -> client.close().get(),
                    "Client should report successful closure on subsequent calls to close().");
        } finally {
            client.close().get();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testSendNotificationAfterClose(final boolean useTokenAuthentication) throws Exception {
        final MockApnsServer server = this.buildServer(new AcceptAllPushNotificationHandlerFactory());
        final ApnsClient client = useTokenAuthentication ?
                this.buildTokenAuthenticationClient() : this.buildTlsAuthenticationClient();

        try {
            server.start(PORT).get();
            client.close().get();

            final SimpleApnsPushNotification pushNotification =
                    new SimpleApnsPushNotification(DEVICE_TOKEN, TOPIC, PAYLOAD);

            assertThrows(ExecutionException.class, () -> client.sendNotification(pushNotification).get(),
                    "Once a client has closed, attempts to send push notifications should fail.");
        } finally {
            client.close().get();
            server.shutdown().get();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testSendNotification(final boolean useTokenAuthentication) throws Exception {
        final ValidatingPushNotificationHandlerFactory handlerFactory = new ValidatingPushNotificationHandlerFactory(
                DEVICE_TOKENS_BY_TOPIC, EXPIRATION_TIMESTAMPS_BY_DEVICE_TOKEN, this.verificationKeysByKeyId,
                this.topicsByVerificationKey);

        final MockApnsServer server = this.buildServer(handlerFactory);
        final ApnsClient client = useTokenAuthentication ?
                this.buildTokenAuthenticationClient() : this.buildTlsAuthenticationClient();

        try {
            server.start(PORT).get();

            final SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(DEVICE_TOKEN, TOPIC, PAYLOAD);

            final PushNotificationResponse<SimpleApnsPushNotification> response =
                    client.sendNotification(pushNotification).get();

            assertTrue(response.isAccepted(),
                    "Clients must send notifications that conform to the APNs protocol specification.");

            assertNotNull(response.getApnsId());
        } finally {
            client.close().get();
            server.shutdown().get();
        }
    }

    @Test
    void testSendNotificationWithExpiredAuthenticationToken() throws Exception {
        final PushNotificationHandlerFactory expireFirstTokenHandlerFactory =
                sslSession -> (PushNotificationHandler) (headers, payload) -> {
                    throw new RejectedNotificationException(RejectionReason.EXPIRED_PROVIDER_TOKEN);
                };

        final MockApnsServer server = this.buildServer(expireFirstTokenHandlerFactory);

        final TestClientMetricsListener metricsListener = new TestClientMetricsListener();
        final ApnsClient client = this.buildTokenAuthenticationClient(metricsListener);

        try {
            server.start(PORT).get();

            final SimpleApnsPushNotification pushNotification =
                    new SimpleApnsPushNotification(DEVICE_TOKEN, TOPIC, PAYLOAD);

            final PushNotificationResponse<SimpleApnsPushNotification> response =
                    client.sendNotification(pushNotification).get();

            assertFalse(response.isAccepted(),
                    "Expired token failures should be fatal.");
        } finally {
            client.close().get();
            server.shutdown().get();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testSendManyNotifications(final boolean useTokenAuthentication) throws Exception {
        final int notificationCount = 1000;

        final List<SimpleApnsPushNotification> pushNotifications = new ArrayList<>();

        for (int i = 0; i < notificationCount; i++) {
            final String token = ApnsClientTest.generateRandomDeviceToken();
            final String payload = ApnsClientTest.generateRandomPayload();

            pushNotifications.add(new SimpleApnsPushNotification(token, TOPIC, payload));
        }

        final MockApnsServer server = this.buildServer(new AcceptAllPushNotificationHandlerFactory());
        final ApnsClient client = useTokenAuthentication ?
                this.buildTokenAuthenticationClient() : this.buildTlsAuthenticationClient();

        try {
            server.start(PORT).get();

            final List<CompletableFuture<PushNotificationResponse<SimpleApnsPushNotification>>> futures =
                    new ArrayList<>();

            for (final SimpleApnsPushNotification pushNotification : pushNotifications) {
                futures.add(client.sendNotification(pushNotification));
            }

            //noinspection ZeroLengthArrayAllocation
            assertDoesNotThrow(() -> CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get());
        } finally {
            client.close().get();
            server.shutdown().get();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testSendManyNotificationsWithListeners(final boolean useTokenAuthentication) throws Exception {
        final int notificationCount = 1000;

        final List<SimpleApnsPushNotification> pushNotifications = new ArrayList<>();

        for (int i = 0; i < notificationCount; i++) {
            final String token = ApnsClientTest.generateRandomDeviceToken();
            final String payload = ApnsClientTest.generateRandomPayload();

            pushNotifications.add(new SimpleApnsPushNotification(token, TOPIC, payload));
        }

        final MockApnsServer server = this.buildServer(new AcceptAllPushNotificationHandlerFactory());
        final ApnsClient client = useTokenAuthentication ?
                this.buildTokenAuthenticationClient() : this.buildTlsAuthenticationClient();

        final CountDownLatch countDownLatch = new CountDownLatch(notificationCount);

        try {
            server.start(PORT).get();

            for (final SimpleApnsPushNotification pushNotification : pushNotifications) {
                client.sendNotification(pushNotification).thenRun(countDownLatch::countDown);
            }

            countDownLatch.await();
        } finally {
            client.close().get();
            server.shutdown().get();
        }
    }

    // See https://github.com/jchambers/pushy/issues/256
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testRepeatedlySendSameNotification(final boolean useTokenAuthentication) throws Exception {
        final int notificationCount = 1000;

        final SimpleApnsPushNotification pushNotification =
                new SimpleApnsPushNotification(DEVICE_TOKEN, TOPIC, PAYLOAD);

        final CountDownLatch countDownLatch = new CountDownLatch(notificationCount);

        final MockApnsServer server = this.buildServer(new AcceptAllPushNotificationHandlerFactory());
        final ApnsClient client = useTokenAuthentication ?
                this.buildTokenAuthenticationClient() : this.buildTlsAuthenticationClient();

        try {
            server.start(PORT).get();

            for (int i = 0; i < notificationCount; i++) {
                // All we're concerned with here is that the client told us SOMETHING about what happened to the
                // notification
                client.sendNotification(pushNotification).whenComplete((response, cause) -> countDownLatch.countDown());
            }

            countDownLatch.await();
        } finally {
            client.close().get();
            server.shutdown().get();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testSendNotificationWithExpiredDeviceToken(final boolean useTokenAuthentication) throws Exception {
        final Instant expiration = Instant.now();

        final PushNotificationHandlerFactory handlerFactory = sslSession -> (headers, payload) -> {
            throw new UnregisteredDeviceTokenException(expiration);
        };

        final SimpleApnsPushNotification pushNotification =
                new SimpleApnsPushNotification(DEVICE_TOKEN, TOPIC, PAYLOAD);

        final MockApnsServer server = this.buildServer(handlerFactory);
        final ApnsClient client = useTokenAuthentication ?
                this.buildTokenAuthenticationClient() : this.buildTlsAuthenticationClient();

        try {
            server.start(PORT).get();

            final PushNotificationResponse<SimpleApnsPushNotification> response =
                    client.sendNotification(pushNotification).get();

            assertFalse(response.isAccepted());
            assertEquals("Unregistered", response.getRejectionReason());
            assertEquals(expiration.toEpochMilli(), response.getTokenInvalidationTimestamp().map(Instant::toEpochMilli).orElse(0L));
        } finally {
            client.close().get();
            server.shutdown().get();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testAcceptedNotificationAndAddedConnectionMetrics(final boolean useTokenAuthentication) throws Exception {
        final MockApnsServer server = this.buildServer(new AcceptAllPushNotificationHandlerFactory());

        final TestClientMetricsListener metricsListener = new TestClientMetricsListener();
        final ApnsClient client = useTokenAuthentication ?
                this.buildTokenAuthenticationClient(metricsListener) : this.buildTlsAuthenticationClient(metricsListener);

        try {
            server.start(PORT).get();

            final SimpleApnsPushNotification pushNotification =
                    new SimpleApnsPushNotification(DEVICE_TOKEN, TOPIC, PAYLOAD);

            client.sendNotification(pushNotification).get();

            metricsListener.waitForNonZeroAcceptedNotifications();

            assertEquals(1, metricsListener.getSentNotifications().size());
            assertEquals(metricsListener.getSentNotifications(), metricsListener.getAcceptedNotifications());
            assertTrue(metricsListener.getRejectedNotifications().isEmpty());

            assertEquals(1, metricsListener.getConnectionsAdded().get());
            assertEquals(0, metricsListener.getConnectionsRemoved().get());
            assertEquals(0, metricsListener.getFailedConnectionAttempts().get());
        } finally {
            client.close().get();
            server.shutdown().get();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testRejectedNotificationMetrics(final boolean useTokenAuthentication) throws Exception {
        final PushNotificationHandlerFactory handlerFactory = sslSession -> (headers, payload) -> {
            throw new RejectedNotificationException(RejectionReason.BAD_DEVICE_TOKEN);
        };

        final MockApnsServer server = this.buildServer(handlerFactory);

        final TestClientMetricsListener metricsListener = new TestClientMetricsListener();
        final ApnsClient client = useTokenAuthentication ?
                this.buildTokenAuthenticationClient(metricsListener) : this.buildTlsAuthenticationClient(metricsListener);

        try {
            server.start(PORT).get();

            final SimpleApnsPushNotification pushNotification =
                    new SimpleApnsPushNotification(DEVICE_TOKEN, TOPIC, PAYLOAD);

            client.sendNotification(pushNotification).get();

            metricsListener.waitForNonZeroRejectedNotifications();

            assertEquals(1, metricsListener.getSentNotifications().size());
            assertEquals(metricsListener.getSentNotifications(), metricsListener.getRejectedNotifications());
            assertTrue(metricsListener.getAcceptedNotifications().isEmpty());
        } finally {
            client.close().get();
            server.shutdown().get();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testFailedConnectionAndWriteFailureMetrics(final boolean useTokenAuthentication) throws Exception {
        final TestClientMetricsListener metricsListener = new TestClientMetricsListener();

        final ApnsClient client = useTokenAuthentication ?
                this.buildTokenAuthenticationClient(metricsListener) : this.buildTlsAuthenticationClient(metricsListener);

        try {
            final SimpleApnsPushNotification pushNotification =
                    new SimpleApnsPushNotification(DEVICE_TOKEN, TOPIC, PAYLOAD);

            // This should fail because there's no server running
            assertThrows(ExecutionException.class, () -> client.sendNotification(pushNotification).get());

            metricsListener.waitForNonZeroFailedConnections();
            metricsListener.waitForNonZeroWriteFailures();

            assertEquals(0, metricsListener.getConnectionsAdded().get());
            assertEquals(0, metricsListener.getConnectionsRemoved().get());
            assertEquals(1, metricsListener.getFailedConnectionAttempts().get());

            assertEquals(1, metricsListener.getWriteFailures().size());
        } finally {
            client.close().get();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testRepeatedlySendNotificationAfterConnectionFailure(final boolean useTokenAuthentication) throws Exception {
        final ApnsClient client = useTokenAuthentication ?
                this.buildTokenAuthenticationClient() : this.buildTlsAuthenticationClient();

        try {
            final SimpleApnsPushNotification pushNotification =
                    new SimpleApnsPushNotification(DEVICE_TOKEN, TOPIC, PAYLOAD);

            for (int i = 0; i < 3; i++) {
                // We should see delays of roughly 0, 1, and 2 seconds; 4 seconds per notification is excessive, but
                // better to play it safe with a timed assertion.
                final CompletableFuture<PushNotificationResponse<SimpleApnsPushNotification>> sendFuture =
                        client.sendNotification(pushNotification);

                // This should fail because there's no server running
                assertThrows(ExecutionException.class, () -> sendFuture.get(4, TimeUnit.SECONDS));
            }
        } finally {
            client.close().get();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testRepeatedlySendNotificationAfterConnectionFailureWithListeners(final boolean useTokenAuthentication) throws Exception {
        final ApnsClient client = useTokenAuthentication ?
                this.buildTokenAuthenticationClient() : this.buildTlsAuthenticationClient();

        try {
            final SimpleApnsPushNotification pushNotification =
                    new SimpleApnsPushNotification(DEVICE_TOKEN, TOPIC, PAYLOAD);

            final int notificationCount = 3;

            final CountDownLatch countDownLatch = new CountDownLatch(notificationCount);

            for (int i = 0; i < notificationCount; i++) {
                client.sendNotification(pushNotification).whenComplete((response, cause) -> countDownLatch.countDown());
            }

            // We should see delays of roughly 0, 1, and 2 seconds (for a total of 3 seconds); waiting 6 seconds in
            // total is overkill, but it's best to leave significant margin on timed assertions.
            assertTrue(countDownLatch.await(6, TimeUnit.SECONDS));
        } finally {
            client.close().get();
        }
    }

    @ParameterizedTest
    @MethodSource("getParametersForTestSendNotificationWithPushTypeHeader")
    void testSendNotificationWithPushTypeHeader(final PushType pushType) throws Exception {
        final ValidatingPushNotificationHandlerFactory handlerFactory = new ValidatingPushNotificationHandlerFactory(
                DEVICE_TOKENS_BY_TOPIC, EXPIRATION_TIMESTAMPS_BY_DEVICE_TOKEN, this.verificationKeysByKeyId,
                this.topicsByVerificationKey);

        final TestParsingServerHandler parsingServerHandler = new TestParsingServerHandler();

        final MockApnsServer server = this.buildServer(handlerFactory, parsingServerHandler);

        final ApnsClient client = this.buildTokenAuthenticationClient();

        try {
            server.start(PORT).get();

            final SimpleApnsPushNotification pushNotification =
                    new SimpleApnsPushNotification(DEVICE_TOKEN, TOPIC, PAYLOAD, null, DeliveryPriority.IMMEDIATE, pushType);

            final PushNotificationResponse<SimpleApnsPushNotification> response =
                    client.sendNotification(pushNotification).get();

            assertTrue(response.isAccepted(),
                    "Clients must send notifications that conform to the APNs protocol specification.");

            parsingServerHandler.waitForNonZeroAcceptedNotifications();

            assertEquals(pushType, parsingServerHandler.acceptedNotifications.get(0).getPushType());
        } finally {
            client.close().get();
            server.shutdown().get();
        }
    }

    private static Stream<Arguments> getParametersForTestSendNotificationWithPushTypeHeader() {
        return Stream.of(
                arguments((Object) null),
                arguments(PushType.BACKGROUND),
                arguments(PushType.ALERT),
                arguments(PushType.VOIP),
                arguments(PushType.COMPLICATION),
                arguments(PushType.FILEPROVIDER),
                arguments(PushType.MDM));
    }
}
