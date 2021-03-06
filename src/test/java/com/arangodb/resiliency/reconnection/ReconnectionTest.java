package com.arangodb.resiliency.reconnection;

import ch.qos.logback.classic.Level;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDBMultipleException;
import com.arangodb.Protocol;
import com.arangodb.resiliency.SingleServerTest;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import eu.rekawek.toxiproxy.model.toxic.Latency;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * @author Michele Rastelli
 */
class ReconnectionTest extends SingleServerTest {

    static Stream<ArangoDB> arangoProvider() {
        return Stream.of(
                dbBuilder().timeout(1_000).useProtocol(Protocol.VST).build(),
                dbBuilder().timeout(1_000).useProtocol(Protocol.HTTP_VPACK).build()
        );
    }

    /**
     * on reconnection failure:
     * - 3x logs WARN Could not connect to host[addr=127.0.0.1,port=8529]
     * - ArangoDBException("Cannot contact any host")
     * <p>
     * once the proxy is re-enabled:
     * - the subsequent requests should be successful
     */
    @ParameterizedTest
    @MethodSource("arangoProvider")
    void unreachableHost(ArangoDB arangoDB) throws IOException, InterruptedException {
        arangoDB.getVersion();

        // close the driver connection
        getEndpoint().getProxy().disable();
        Thread.sleep(100);

        for (int i = 0; i < 10; i++) {
            Throwable thrown = catchThrowable(arangoDB::getVersion);
            assertThat(thrown).isInstanceOf(ArangoDBException.class);
            assertThat(thrown.getMessage()).contains("Cannot contact any host");
            assertThat(thrown.getCause()).isNotNull();
            assertThat(thrown.getCause()).isInstanceOf(ArangoDBMultipleException.class);
            ((ArangoDBMultipleException) thrown.getCause()).getExceptions().forEach(e -> {
                assertThat(e).isInstanceOf(ConnectException.class);
            });
        }

        long warnsCount = logs.getLoggedEvents().stream()
                .filter(e -> e.getLevel().equals(Level.WARN))
                .filter(e -> e.getMessage().contains("Could not connect to host[addr=127.0.0.1,port=8529]"))
                .count();
        assertThat(warnsCount).isGreaterThanOrEqualTo(3);

        getEndpoint().getProxy().enable();
        Thread.sleep(100);

        arangoDB.getVersion();
        arangoDB.shutdown();
    }

    /**
     * on reconnection failure:
     * - 3x logs WARN Could not connect to host[addr=127.0.0.1,port=8529]
     * - ArangoDBException("Cannot contact any host")
     * <p>
     * once the proxy is re-enabled:
     * - the subsequent requests should be successful
     */
    @ParameterizedTest
    @MethodSource("arangoProvider")
    void connectionTimeout(ArangoDB arangoDB) throws IOException, InterruptedException {
        arangoDB.getVersion();

        // slow down the driver connection
        Latency toxic = getEndpoint().getProxy().toxics().latency("latency", ToxicDirection.DOWNSTREAM, 10_000);
        Thread.sleep(100);

        Throwable thrown = catchThrowable(arangoDB::getVersion);
        thrown.printStackTrace();
        assertThat(thrown)
                .isInstanceOf(ArangoDBException.class)
                .extracting(Throwable::getCause)
                .isInstanceOf(TimeoutException.class);

        toxic.remove();
        Thread.sleep(100);

        arangoDB.getVersion();
        arangoDB.shutdown();
    }

}
