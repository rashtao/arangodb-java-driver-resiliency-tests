package com.arangodb.resiliency.connection;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDBMultipleException;
import com.arangodb.Protocol;
import com.arangodb.resiliency.SingleServerTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * @author Michele Rastelli
 */
class ConnectionTest extends SingleServerTest {

    static Stream<Protocol> protocolProvider() {
        return Stream.of(
                Protocol.VST,
                Protocol.HTTP_VPACK
        );
    }

    static Stream<ArangoDB> arangoProvider() {
        return Stream.of(
                dbBuilder().useProtocol(Protocol.VST).build(),
                dbBuilder().useProtocol(Protocol.HTTP_VPACK).build()
        );
    }

    @ParameterizedTest
    @MethodSource("protocolProvider")
    void nameResolutionFailTest(Protocol protocol) {
        ArangoDB arangoDB = new ArangoDB.Builder()
                .host("wrongHost", 8529)
                .useProtocol(protocol)
                .build();

        Throwable thrown = catchThrowable(arangoDB::getVersion);
        assertThat(thrown).isInstanceOf(ArangoDBException.class);
        assertThat(thrown.getMessage()).contains("Cannot contact any host!");
        assertThat(thrown.getCause()).isNotNull();
        assertThat(thrown.getCause()).isInstanceOf(ArangoDBMultipleException.class);
        ((ArangoDBMultipleException) thrown.getCause()).getExceptions().forEach(e -> {
            assertThat(e).isInstanceOf(UnknownHostException.class);
            assertThat(e.getMessage()).contains("wrongHost");
        });
        arangoDB.shutdown();
    }

    @ParameterizedTest
    @MethodSource("arangoProvider")
    void connectionFailTest(ArangoDB arangoDB) throws IOException, InterruptedException {
        getEndpoint().getProxy().disable();
        Thread.sleep(100);

        Throwable thrown = catchThrowable(arangoDB::getVersion);
        assertThat(thrown).isInstanceOf(ArangoDBException.class);
        assertThat(thrown.getMessage()).contains("Cannot contact any host");
        assertThat(thrown.getCause()).isNotNull();
        assertThat(thrown.getCause()).isInstanceOf(ArangoDBMultipleException.class);
        ((ArangoDBMultipleException) thrown.getCause()).getExceptions().forEach(e -> {
            assertThat(e).isInstanceOf(ConnectException.class);
        });
        arangoDB.shutdown();
        getEndpoint().getProxy().enable();
        Thread.sleep(100);
    }

}
