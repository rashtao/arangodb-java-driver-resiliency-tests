package keepAlive;

import com.arangodb.ArangoDB;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Start toxiproxy server before running
 *
 * @author Michele Rastelli
 */
class ReconnectionTest {

    private Proxy proxy;
    private ToxiproxyClient client;
    private ArangoDB arangoDB;

    @BeforeEach
    void init() throws IOException {
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

        // delete and recreate proxy, closes the driver connection
        proxy.delete();
        proxy = client.createProxy("arango", "127.0.0.1:8529", "172.28.3.1:8529");

        arangoDB.getVersion();
    }
}
