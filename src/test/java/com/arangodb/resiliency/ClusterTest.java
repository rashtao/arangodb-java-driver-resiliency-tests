package com.arangodb.resiliency;

import ch.qos.logback.classic.Level;
import com.arangodb.ArangoDB;
import com.arangodb.resiliency.utils.MemoryAppender;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import java.io.IOException;
import java.util.List;

@Tag("cluster")
public abstract class ClusterTest {

    protected static final String HOST = "127.0.0.1";
    protected static final String PASSWORD = "test";
    protected static final MemoryAppender logs = new MemoryAppender(Level.WARN);
    private static final List<Endpoint> endpoints = List.of(
            new Endpoint("cluster1", HOST, 8529, "172.28.13.1:8529"),
            new Endpoint("cluster2", HOST, 8539, "172.28.13.2:8529"),
            new Endpoint("cluster3", HOST, 8549, "172.28.13.3:8529")
    );

    @BeforeAll
    static void beforeAll() throws IOException {
        ToxiproxyClient client = new ToxiproxyClient(HOST, 8474);
        for (Endpoint ph : endpoints) {
            Proxy p = client.getProxyOrNull(ph.getName());
            if (p != null) {
                p.delete();
            }
            ph.setProxy(client.createProxy(ph.getName(), ph.getHost() + ":" + ph.getPort(), ph.getUpstream()));
        }
    }

    @AfterAll
    static void afterAll() throws IOException {
        for (Endpoint ph : endpoints) {
            ph.getProxy().delete();
        }
    }

    @BeforeEach
    void beforeEach() throws IOException {
        for (Endpoint ph : endpoints) {
            ph.getProxy().enable();
        }
    }

    protected static List<Endpoint> getEndpoints() {
        return endpoints;
    }

    protected static ArangoDB.Builder dbBuilder() {
        ArangoDB.Builder builder = new ArangoDB.Builder().password(PASSWORD);
        for (Endpoint ph : endpoints) {
            builder.host(ph.getHost(), ph.getPort());
        }
        return builder;
    }

}
