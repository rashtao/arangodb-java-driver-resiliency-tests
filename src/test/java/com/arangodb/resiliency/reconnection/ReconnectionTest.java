package com.arangodb.resiliency.reconnection;

import ch.qos.logback.classic.Level;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.Protocol;
import com.arangodb.resiliency.SingleServerTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * @author Michele Rastelli
 */
class ReconnectionTest extends SingleServerTest {

    static Stream<ArangoDB> arangoProvider() {
        return Stream.of(
                dbBuilder().useProtocol(Protocol.VST).build(),
                dbBuilder().useProtocol(Protocol.HTTP_VPACK).build()
        );
    }

    /**
     * if the proxy is disabled:
     * - the subsequent requests should throw ArangoDBException("Cannot contact any host")
     * <p>
     * once the proxy is re-enabled:
     * - the subsequent requests should be successful
     */
    @ParameterizedTest
    @MethodSource("arangoProvider")
    void closeAndReopenConnection(ArangoDB arangoDB) throws IOException, InterruptedException {
        arangoDB.getVersion();

        // closes the driver connection
        getProxy().disable();
        Thread.sleep(100);

        Throwable thrown = catchThrowable(arangoDB::getVersion);
        assertThat(thrown).isInstanceOf(ArangoDBException.class);
        assertThat(thrown.getMessage()).contains("Cannot contact any host");

        getProxy().enable();
        Thread.sleep(100);

        arangoDB.getVersion();
        arangoDB.shutdown();
    }

    /**
     * on reconnection failure:
     * - 3x logs WARN Could not connect to host[addr=127.0.0.1,port=8529]
     * - ArangoDBException("Cannot contact any host")
     */
    @ParameterizedTest
    @MethodSource("arangoProvider")
    void closeConnection(ArangoDB arangoDB) throws IOException {
        arangoDB.getVersion();

        // close the driver connection
        getProxy().disable();

        Throwable thrown = catchThrowable(arangoDB::getVersion);
        assertThat(thrown).isInstanceOf(ArangoDBException.class);
        assertThat(thrown.getMessage()).contains("Cannot contact any host");

        long warnsCount = memoryAppender.getLoggedEvents().stream()
                .filter(e -> e.getMessage().contains("Could not connect to host[addr=127.0.0.1,port=8529]") &&
                        e.getLevel().equals(Level.WARN))
                .count();
        assertThat(warnsCount).isGreaterThanOrEqualTo(3);
        arangoDB.shutdown();
    }

}
