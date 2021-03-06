package com.arangodb.resiliency.vstKeepAlive;

import ch.qos.logback.classic.Level;
import com.arangodb.async.ArangoDBAsync;
import com.arangodb.resiliency.SingleServerTest;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import eu.rekawek.toxiproxy.model.toxic.Latency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.awaitility.Awaitility.await;

/**
 * @author Michele Rastelli
 */
class VstKeepAliveCloseAsyncTest extends SingleServerTest {

    private ArangoDBAsync arangoDB;

    @BeforeEach
    void init() {
        arangoDB = dbBuilderAsync()
                .timeout(1000)
                .keepAliveInterval(1)
                .build();
    }

    @AfterEach
    void shutDown() {
        arangoDB.shutdown();
    }

    /**
     * after 3 consecutive VST keepAlive failures:
     * - log ERROR Connection unresponsive
     * - reconnect on next request
     */
    @Test
    @Timeout(10)
    void keepAliveCloseAndReconnect() throws IOException, ExecutionException, InterruptedException {
        arangoDB.getVersion().get();
        Latency toxic = getEndpoint().getProxy().toxics().latency("latency", ToxicDirection.DOWNSTREAM, 10_000);
        await().until(() -> logs.getLoggedEvents().stream()
                .filter(e -> e.getLevel().equals(Level.ERROR))
                .anyMatch(e -> e.getMessage().contains("Connection unresponsive!")));
        toxic.setLatency(0L);
        arangoDB.getVersion().get();
    }
}
