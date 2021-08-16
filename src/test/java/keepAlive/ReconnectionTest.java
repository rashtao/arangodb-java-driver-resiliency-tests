package keepAlive;

import ch.qos.logback.classic.Level;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.MemoryAppender;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Start toxiproxy server before running
 *
 * @author Michele Rastelli
 */
class ReconnectionTest {

    private Proxy proxy;
    private ToxiproxyClient client;
    private ArangoDB arangoDB;
    private MemoryAppender memoryAppender;

    @BeforeEach
    void init() throws IOException {
        memoryAppender = new MemoryAppender(Level.WARN);
        client = new ToxiproxyClient("127.0.0.1", 8474);
        proxy = client.createProxy("arango", "127.0.0.1:8529", "172.28.3.1:8529");
        arangoDB = new ArangoDB.Builder()
                .host("127.0.0.1", 8529)
                .password("test")
                .timeout(60_000)
                .build();
    }

    @AfterEach
    void shutDown() throws IOException {
        proxy.delete();
        arangoDB.shutdown();
    }

    /**
     * the VST connection should be closed and reopened, without throwing any exception
     */
    @Test
    void closeAndReopenConnection() throws IOException {
        arangoDB.getVersion();

        // closes the driver connection
        proxy.disable();
        proxy.enable();

        arangoDB.getVersion();
    }

    /**
     * log warnings produced by reconnection failure
     */
    @Test
    void closeConnection() throws IOException {
        arangoDB.getVersion();

        // closes the driver connection
        proxy.disable();

        Throwable thrown = catchThrowable(() -> arangoDB.getVersion());
        assertThat(thrown).isInstanceOf(ArangoDBException.class);
        assertThat(thrown.getMessage()).contains("Cannot contact any host");

        long warnsCount = memoryAppender.getLoggedEvents().stream()
                .filter(e -> e.getMessage().contains("Could not connect to host[addr=127.0.0.1,port=8529]") &&
                        e.getLevel().equals(Level.WARN))
                .count();
        assertThat(warnsCount >= 3);
    }

}
