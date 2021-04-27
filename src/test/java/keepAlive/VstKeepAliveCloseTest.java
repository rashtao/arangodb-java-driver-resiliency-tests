package keepAlive;

import ch.qos.logback.classic.Level;
import com.arangodb.ArangoDB;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import eu.rekawek.toxiproxy.model.toxic.Latency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import utils.MemoryAppender;

import java.io.IOException;

import static org.awaitility.Awaitility.await;

/**
 * Start toxiproxy server before running
 *
 * @author Michele Rastelli
 */
class VstKeepAliveCloseTest {

    private Proxy proxy;
    private ArangoDB arangoDB;
    private MemoryAppender memoryAppender;

    @BeforeEach
    void init() throws IOException {
        memoryAppender = new MemoryAppender(Level.ERROR);

        ToxiproxyClient client = new ToxiproxyClient("127.0.0.1", 8474);
        proxy = client.createProxy("arango", "127.0.0.1:8529", "172.28.3.1:8529");
        arangoDB = new ArangoDB.Builder()
                .host("127.0.0.1", 8529)
                .password("test")
                .timeout(1000)
                .keepAliveInterval(1)
                .build();
    }

    @AfterEach
    void shutDown() throws IOException {
        proxy.delete();
        arangoDB.shutdown();
    }

    /**
     * the VST connection should be closed after 3 consecutive keepAlive failures
     */
    @Test
    @Timeout(10)
    void keepAliveCloseAndReconnect() throws IOException {
        arangoDB.getVersion();
        Latency toxic = proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, 10_000);
        await().until(() -> memoryAppender.getLoggedEvents().stream()
                .anyMatch(e -> e.getMessage().contains("Connection unresponsive!") &&
                        e.getLevel().equals(Level.ERROR)));
        toxic.setLatency(0);
        arangoDB.getVersion();
    }
}
