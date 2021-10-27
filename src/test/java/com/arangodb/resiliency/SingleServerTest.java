package com.arangodb.resiliency;

import ch.qos.logback.classic.Level;
import com.arangodb.ArangoDB;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import org.junit.jupiter.api.AfterAll;
import com.arangodb.resiliency.utils.MemoryAppender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import java.io.IOException;

@Tag("singleServer")
public abstract class SingleServerTest {

    protected static final String HOST = "127.0.0.1";
    protected static final String PASSWORD = "test";
    protected static final MemoryAppender logs = new MemoryAppender(Level.WARN);
    private static final Endpoint endpoint = new Endpoint("singleServer", HOST, 8529, "172.28.3.1:8529");

    @BeforeAll
    static void beforeAll() throws IOException {
        ToxiproxyClient client = new ToxiproxyClient(HOST, 8474);
        Proxy p = client.getProxyOrNull(endpoint.getName());
        if (p != null) {
            p.delete();
        }
        endpoint.setProxy(client.createProxy(endpoint.getName(), HOST + ":" + endpoint.getPort(), endpoint.getUpstream()));
    }

    @AfterAll
    static void afterAll() throws IOException {
        endpoint.getProxy().delete();
    }

    @BeforeEach
    void beforeEach() throws IOException {
        endpoint.getProxy().enable();
    }

    protected static Endpoint getEndpoint() {
        return endpoint;
    }

    protected static ArangoDB.Builder dbBuilder() {
        return new ArangoDB.Builder()
                .host(endpoint.getHost(), endpoint.getPort())
                .password(PASSWORD);
    }

}
