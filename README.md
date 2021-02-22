# pushy

![Build/test](https://github.com/jchambers/pushy/actions/workflows/test.yml/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.eatthepath/pushy/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.eatthepath/pushy)

Pushy is a Java library for sending [APNs](https://developer.apple.com/documentation/usernotifications) (iOS, macOS, and Safari) push notifications.

Pushy sends push notifications using Apple's HTTP/2-based APNs protocol and supports both TLS and token-based authentication. It distinguishes itself from other push notification libraries with a focus on [thorough documentation](https://pushy-apns.org/apidocs/0.14/index.html?overview-summary.html), asynchronous operation, and design for industrial-scale operation; with Pushy, it's easy and efficient to maintain multiple parallel connections to the APNs gateway to send large numbers of notifications to many different applications ("topics").

We believe that Pushy is already the best tool for sending APNs push notifications from Java applications, and we hope you'll help us make it even better via bug reports and pull requests. If you have questions about using Pushy, please join us on [the Pushy mailing list](https://groups.google.com/d/forum/pushy-apns) or take a look at [the wiki](https://github.com/jchambers/pushy/wiki). Thanks!

If you need a simple GUI application for sending push notifications for development or testing purposes, you might also be interested in Pushy's sister project, [Pushy Console](https://github.com/jchambers/pushy-console).

## Getting Pushy

If you use [Maven](http://maven.apache.org/), you can add Pushy to your project by adding the following dependency declaration to your POM:

```xml
<dependency>
    <groupId>com.eatthepath</groupId>
    <artifactId>pushy</artifactId>
    <version>0.14.2</version>
</dependency>
```

If you don't use Maven (or something else that understands Maven dependencies, like Gradle), you can [download Pushy as a `.jar` file](https://github.com/jchambers/pushy/releases/download/pushy-0.14.2/pushy-0.14.2.jar) and add it to your project directly. You'll also need to make sure you have Pushy's runtime dependencies on your classpath. They are:

- [netty 4.1.53](http://netty.io/)
- [gson 2.6](https://github.com/google/gson)
- [slf4j 1.7](http://www.slf4j.org/) (and possibly an SLF4J binding, as described in the [logging](#logging) section below)
- [fast-uuid 0.1](https://github.com/jchambers/fast-uuid)

Pushy itself requires Java 8 or newer to build and run. While not required, users may choose to use [netty-native](http://netty.io/wiki/forked-tomcat-native.html) as an SSL provider for enhanced performance. To use a native provider, make sure netty-tcnative is on your classpath. Maven users may add a dependency to their project as follows:

```xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-tcnative-boringssl-static</artifactId>
    <version>2.0.34.Final</version>
    <scope>runtime</scope>
</dependency>
```

## Authenticating with the APNs server

Before you can get started with Pushy, you'll need to do some provisioning work with Apple to register your app and get the required certificates or signing keys (more on these shortly). For details on this process, please see the [Registering Your App with APNs](https://developer.apple.com/documentation/usernotifications/registering_your_app_with_apns) section of Apple's UserNotifications documentation. Please note that there are [some caveats](https://github.com/jchambers/pushy/wiki/Certificates), particularly under macOS 10.13 (El Capitan).

Generally speaking, APNs clients must authenticate with the APNs server by some means before they can send push notifications. Currently, APNs (and Pushy) supports two authentication methods: TLS-based authentication and token-based authentication. The two approaches are mutually-exclusive; you'll need to pick one or the other for each client.

If your system-wide trust store does not already include it, you may need to add the GeoTrust Global CA root certificate as a trusted certificate:

```java
final ApnsClient apnsClientWithGeoTrustCertificate = new ApnsClientBuilder()
        .setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
        .setTrustedServerCertificateChain(ApnsClientBuilder.getGeoTrustGlobalCaRootCertificate())
        .build();
```

For additional details about trusting the GeoTrust Global CA root, please see TODO.

### TLS authentication

In TLS-based authentication, clients present a TLS certificate to the server when connecting, and may send notifications to any "topic" named in the certificate. Generally, this means that a single client can only send push notifications to a single receiving app.

Once you've registered your app and have the requisite certificates, the first thing you'll need to do to start sending push notifications with Pushy is to create an [`ApnsClient`](https://pushy-apns.org/apidocs/0.14/com/eatthepath/pushy/apns/ApnsClient.html). Clients using TLS authentication need a certificate and private key to authenticate with the APNs server. The most common way to store the certificate and key is in a password-protected PKCS#12 file (you'll wind up with a password-protected .p12 file if you follow Apple's instructions at the time of this writing). To create a client that will use TLS-based authentication:

```java
final ApnsClient apnsClient = new ApnsClientBuilder()
        .setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
        .setClientCredentials(new File("/path/to/certificate.p12"), "p12-file-password")
        .build();
```

### Token authentication

In token-based authentication, clients still connect to the server using a TLS-secured connection, but do *not* present a certificate to the server when connecting. Instead, clients include a cryptographically-signed token with each notification they send (don't worry—Pushy handles this for you automatically). Clients may send push notifications to any "topic" for which they have a valid signing key.

To get started with a token-based client, you'll need to get a signing key (also called a private key in some contexts) from Apple. Once you have your signing key, you can create a new client:

```java
final ApnsClient apnsClient = new ApnsClientBuilder()
        .setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
        .setSigningKey(ApnsSigningKey.loadFromPkcs8File(new File("/path/to/key.p8"),
                "TEAMID1234", "KEYID67890"))
        .build();
```

## Sending push notifications

Pushy's APNs clients maintain an internal pool of connections to the APNs server and create new connections on demand. As a result, clients do not need to be started explicitly. Regardless of the authentication method you choose, once you've created a client, it's ready to start sending push notifications. At minimum, [push notifications](https://pushy-apns.org/apidocs/0.14/com/eatthepath/pushy/apns/ApnsPushNotification.html) need a device token (which identifies the notification's destination device and is a distinct idea from an authentication token), a topic, and a payload.

```java
final SimpleApnsPushNotification pushNotification;

{
    final ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
    payloadBuilder.setAlertBody("Example!");

    final String payload = payloadBuilder.build();
    final String token = TokenUtil.sanitizeTokenString("<efc7492 bdbd8209>");

    pushNotification = new SimpleApnsPushNotification(token, "com.example.myApp", payload);
}
```

Pushy includes a [`SimpleApnsPayloadBuilder`](https://pushy-apns.org/apidocs/0.14/com/eatthepath/pushy/apns/util/SimpleApnsPayloadBuilder.html), and payload builders based on [Gson](https://github.com/jchambers/pushy/tree/master/gson-payload-builder) and [Jackson](https://github.com/jchambers/pushy/tree/master/jackson-payload-builder) are available as separate modules. [APNs payloads are just JSON strings](https://developer.apple.com/documentation/usernotifications/setting_up_a_remote_notification_server/generating_a_remote_notification), and callers may produce payloads by the method of their choice; while Pushy's payload builders may be convenient, callers are _not_ obligated to use them.

The process of sending a push notification is asynchronous; although the process of sending a notification and getting a reply from the server may take some time, the client will return a [`CompletableFuture`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) right away. You can use that `CompletableFuture` to track the progress and eventual outcome of the sending operation. Note that sending a notification returns a [`PushNotificationFuture`](https://pushy-apns.org/apidocs/0.14/com/eatthepath/pushy/apns/util/concurrent/PushNotificationFuture.html), which is a subclass of `CompletableFuture` that always holds a reference to the notification that was sent.

```java
final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
    sendNotificationFuture = apnsClient.sendNotification(pushNotification);
```

The `CompletableFuture` will complete in one of three circumstances:

1. The gateway accepts the notification and will attempt to deliver it to the destination device.
2. The gateway rejects the notification; this should be considered a permanent failure, and the notification should not be sent again. Additionally, the APNs gateway may indicate a timestamp at which the destination token became invalid. If that happens, you should stop trying to send *any* notification to that token unless the token has been re-registered since that timestamp.
3. The `CompletableFuture` fails with an exception. This should generally be considered a temporary failure, and callers should try to send the notification again when the problem has been resolved.

An example:

```java
try {
    final PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse =
            sendNotificationFuture.get();

    if (pushNotificationResponse.isAccepted()) {
        System.out.println("Push notification accepted by APNs gateway.");
    } else {
        System.out.println("Notification rejected by the APNs gateway: " +
                pushNotificationResponse.getRejectionReason());

        pushNotificationResponse.getTokenInvalidationTimestamp().ifPresent(timestamp -> {
            System.out.println("\t…and the token is invalid as of " + timestamp);
        });
    }
} catch (final ExecutionException e) {
    System.err.println("Failed to send push notification.");
    e.printStackTrace();
}
```

It's important to note that `CompletableFuture` has affordances for scheduling additional tasks to run when an operation is complete. Waiting for each individual push notification is inefficient in practice, and most users will be better served by adding follow-up tasks to the `CompletableFuture` instead of blocking until it completes. As an example:

```java
sendNotificationFuture.whenComplete((response, cause) -> {
    if (response != null) {
        // Handle the push notification response as before from here.
    } else {
        // Something went wrong when trying to send the notification to the
        // APNs server. Note that this is distinct from a rejection from
        // the server, and indicates that something went wrong when actually
        // sending the notification or waiting for a reply.
        cause.printStackTrace();
    }
});
```

All APNs clients—even those that have never sent a message—may allocate and hold on to system resources, and it's important to release them. APNs clients are intended to be persistent, long-lived resources; you definitely don't need to shut down a client after sending a notification (or even batch of notifications), but you'll want to shut down your client (or clients) when your application is shutting down:

```java
final CompletableFuture<Void> closeFuture = apnsClient.close();
```

When shutting down, clients will wait for all sent-but-not-acknowledged notifications to receive a reply from the server. Notifications that have been passed to `sendNotification` but not yet sent to the server (i.e. notifications waiting in an internal queue) will fail immediately when disconnecting. Callers should generally make sure that all sent notifications have been acknowledged by the server before shutting down.

## Performance and best practices

Making the most of your system resources for high-throughput applications always takes some effort. To guide you through the process, we've put together a wiki page covering some [best practices for using Pushy](https://github.com/jchambers/pushy/wiki/Best-practices). All of these points are covered in much more detail on the wiki, but in general, our recommendations are:

- Treat `ApnsClient` instances as long-lived resources
- Add follow-up tasks to `CompletableFutures` if you want to track the status of your push notifications
- Use a flow control strategy to avoid enqueueing push notifications faster than the server can respond
- Choose a number of threads and concurrent connections that balances CPU time and network throughput

## Metrics

Pushy includes an interface for monitoring metrics that provide insight into clients' behavior and performance. You can write your own implementation of the `ApnsClientMetricsListener` interface to record and report metrics. We also provide metrics listeners that gather and report metrics [using the Dropwizard Metrics library](https://github.com/jchambers/pushy/tree/master/dropwizard-metrics-listener) and [using the Micrometer application monitoring facade](https://github.com/jchambers/pushy/tree/master/micrometer-metrics-listener) as separate modules. To begin receiving metrics, set a listener when building a new client:

```java
final ApnsClient apnsClient = new ApnsClientBuilder()
        .setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
        .setSigningKey(ApnsSigningKey.loadFromPkcs8File(new File("/path/to/key.p8"),
                "TEAMID1234", "KEYID67890"))
        .setMetricsListener(new MyCustomMetricsListener())
        .build();
```

Please note that the metric-handling methods in your listener implementation should *never* call blocking code. It's appropriate to increment counters directly in the handler methods, but calls to databases or remote monitoring endpoints should be dispatched to separate threads.

## Using a proxy

If you need to use a proxy for outbound connections, you may specify a [`ProxyHandlerFactory`](https://pushy-apns.org/apidocs/0.14/com/eatthepath/pushy/apns/proxy/ProxyHandlerFactory.html) when building your `ApnsClient` instance. Concrete implementations of `ProxyHandlerFactory` are provided for HTTP, SOCKS4, and SOCKS5 proxies.

An example:

```java
final ApnsClient apnsClient = new ApnsClientBuilder()
    .setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
    .setSigningKey(ApnsSigningKey.loadFromPkcs8File(new File("/path/to/key.p8"),
            "TEAMID1234", "KEYID67890"))
    .setProxyHandlerFactory(new Socks5ProxyHandlerFactory(
        new InetSocketAddress("my.proxy.com", 1080), "username", "password"))
    .build();
```

If using HTTP proxies configured via [JVM system properties](https://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html#Proxies), you can also use:

```java
final ApnsClient apnsClient = new ApnsClientBuilder()
    .setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
    .setSigningKey(ApnsSigningKey.loadFromPkcs8File(new File("/path/to/key.p8"),
            "TEAMID1234", "KEYID67890"))
    .setProxyHandlerFactory(HttpProxyHandlerFactory.fromSystemProxies(
            ApnsClientBuilder.DEVELOPMENT_APNS_HOST))
    .build();
```

## Logging

Pushy uses [SLF4J](http://www.slf4j.org/) for logging. If you're not already familiar with it, SLF4J is a facade that allows users to choose which logging library to use at deploy time by adding a specific "binding" to the classpath. To avoid making the choice for you, Pushy itself does *not* depend on any SLF4J bindings; you'll need to add one on your own (either by adding it as a dependency in your own project or by installing it directly). If you have no SLF4J bindings on your classpath, you'll probably see a warning that looks something like this:

```
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
```

For more information, see the [SLF4J user manual](http://www.slf4j.org/manual.html).

Pushy uses logging levels as follows:

| Level     | Events logged                                                                         |
|-----------|---------------------------------------------------------------------------------------|
| `error`   | Serious, unrecoverable errors; recoverable errors that likely indicate a bug in Pushy |
| `warn`    | Serious, but recoverable errors; errors that may indicate a bug in caller's code      |
| `info`    | Important lifecycle events                                                            |
| `debug`   | Minor lifecycle events; expected exceptions                                           |
| `trace`   | Individual IO operations                                                              |

## Using a mock server

Pushy includes a mock APNs server that callers may use in integration tests and benchmarks. It is not necessary to use a mock server (or any related classes) in normal operation.

To build a mock server, callers should use a [`MockApnsServerBuilder`](https://pushy-apns.org/apidocs/0.14/com/eatthepath/pushy/apns/server/MockApnsServerBuilder.html). All servers require a [`PushNotificationHandler`](https://pushy-apns.org/apidocs/0.14/com/eatthepath/pushy/apns/server/PushNotificationHandler.html) (built by a [`PushNotificationHandlerFactory`](https://pushy-apns.org/apidocs/0.14/com/eatthepath/pushy/apns/server/PushNotificationHandlerFactory.html) provided to the builder) that decides whether the mock server will accept or reject each incoming push notification. Pushy includes an `AcceptAllPushNotificationHandlerFactory` that is helpful for benchmarking and a `ValidatingPushNotificationHandlerFactory` that may be helpful for integration testing.

Callers may also provide a [`MockApnsServerListener`](https://pushy-apns.org/apidocs/0.14/com/eatthepath/pushy/apns/server/MockApnsServerListener.html) when building a mock server; listeners are notified whenever the mock server accepts or rejects a notification from a client.

## License and status

Pushy is available under the [MIT License](https://github.com/jchambers/pushy/blob/master/LICENSE.md).

The current version of Pushy is 0.14.2. It's fully functional and widely used in production environments, but the public API may change significantly before a 1.0 release.
