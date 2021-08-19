package com.arangodb.resiliency;

import ch.qos.logback.classic.Level;
import com.arangodb.ArangoDB;
import com.arangodb.async.ArangoDBAsync;
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
    protected static final int PORT = 8529;
    protected static final String PASSWORD = "test";
    protected static final MemoryAppender memoryAppender = new MemoryAppender(Level.WARN);
    private static Proxy proxy;
    private static final String UPSTREAM_HOST = "172.28.3.1";
    private static final String PROXY_NAME = "singleServer";

    @BeforeAll
    static void beforeAll() throws IOException {
        ToxiproxyClient client = new ToxiproxyClient(HOST, 8474);
        Proxy p = client.getProxyOrNull(PROXY_NAME);
        if (p != null) {
            p.delete();
        }
        proxy = client.createProxy(PROXY_NAME, HOST + ":" + PORT, UPSTREAM_HOST + ":" + PORT);
    }

    @AfterAll
    static void afterAll() throws IOException {
        proxy.delete();
    }

    @BeforeEach
    void beforeEach() throws IOException {
        getProxy().enable();
    }

    protected static Proxy getProxy() {
        return proxy;
    }

    protected static ArangoDB.Builder dbBuilder() {
        return new ArangoDB.Builder()
                .host(HOST, PORT)
                .password(PASSWORD);
    }

    protected static ArangoDBAsync.Builder dbBuilderAsync() {
        return new ArangoDBAsync.Builder()
                .host(HOST, PORT)
                .password(PASSWORD);
    }

}
