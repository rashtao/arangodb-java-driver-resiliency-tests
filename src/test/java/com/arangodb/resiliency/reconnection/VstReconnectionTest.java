package com.arangodb.resiliency.reconnection;

import ch.qos.logback.classic.Level;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.resiliency.SingleServerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * @author Michele Rastelli
 */
class VstReconnectionTest extends SingleServerTest {

    private ArangoDB arangoDB;

    @BeforeEach
    void init() {
        arangoDB = dbBuilder().build();
    }

    @AfterEach
    void shutDown() {
        arangoDB.shutdown();
    }

    /**
     * if the proxy is disabled:
     * - the subsequent requests should throw ArangoDBException("Cannot contact any host")
     *
     * once the proxy is re-enabled:
     * - the subsequent requests should be successful
     */
    @Test
    void closeAndReopenConnection() throws IOException, InterruptedException {
        arangoDB.getVersion();

        // closes the driver connection
        getProxy().disable();
        Thread.sleep(100);

        Throwable thrown = catchThrowable(() -> arangoDB.getVersion());
        assertThat(thrown).isInstanceOf(ArangoDBException.class);
        assertThat(thrown.getMessage()).contains("Cannot contact any host");

        getProxy().enable();
        Thread.sleep(100);

        arangoDB.getVersion();
    }

    /**
     * on VST reconnection failure:
     * - 3x logs WARN Could not connect to host[addr=127.0.0.1,port=8529]
     * - ArangoDBException("Cannot contact any host")
     */
    @Test
    void closeConnection() throws IOException {
        arangoDB.getVersion();

        // closes the driver connection
        getProxy().disable();

        Throwable thrown = catchThrowable(() -> arangoDB.getVersion());
        assertThat(thrown).isInstanceOf(ArangoDBException.class);
        assertThat(thrown.getMessage()).contains("Cannot contact any host");

        long warnsCount = memoryAppender.getLoggedEvents().stream()
                .filter(e -> e.getMessage().contains("Could not connect to host[addr=127.0.0.1,port=8529]") &&
                        e.getLevel().equals(Level.WARN))
                .count();
        assertThat(warnsCount).isGreaterThanOrEqualTo(3);
    }

}
