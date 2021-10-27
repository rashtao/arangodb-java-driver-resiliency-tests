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
     * on reconnection failure:
     * - 3x logs WARN Could not connect to host[addr=127.0.0.1,port=8529]
     * - ArangoDBException("Cannot contact any host")
     * <p>
     * once the proxy is re-enabled:
     * - the subsequent requests should be successful
     */
    @ParameterizedTest
    @MethodSource("arangoProvider")
    void closeAndReconnect(ArangoDB arangoDB) throws IOException, InterruptedException {
        arangoDB.getVersion();

        // close the driver connection
        getEndpoint().getProxy().disable();
        Thread.sleep(100);

        for (int i = 0; i < 10; i++) {
            Throwable thrown = catchThrowable(arangoDB::getVersion);
            assertThat(thrown).satisfiesAnyOf(
                    t -> assertThat(t).isInstanceOf(ArangoDBException.class),
                    t -> assertThat(t).isInstanceOf(NullPointerException.class)
            );
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

}
