package com.example;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest
class HelloModelTest {

    /**
     * Verifies that calling sendMessage() on the model correctly delegates the
     * message string to the underlying NtfyConnection implementation.
     * The model's internal messageToSend() is ignored in this test using a spy.
     */
    @Test
    void sendMessageCallsConnectionWithMessageToSend() {
        //Arrange
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);

        //Act
        model.setMessageToSend("");
        model.sendMessage("Hello World");

        //Assert
        assertThat(spy.message).isEqualTo("Hello World");
    }

    /**
     * Tests the message receiving functionality by simulating a JSON stream from
     * a fake Ntfy server using WireMock.
     * Verifies that the model filters out 'keepalive' events and correctly processes
     * the 'message' event, adding it to the ObservableList.
     * Uses a polling loop to wait for the asynchronous message processing to complete.
     *
     * @param wireMockRuntimeInfo Provides port information for the simulated server.
     * @throws IOException          If an I/O error occurs during setup.
     * @throws InterruptedException If the waiting thread is interrupted.
     */
    @Test
    void receiveMessageFromFakeServer(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException, InterruptedException {
        //Arrange
        var host = new NtfyConnectionImpl("http://localhost:" + wireMockRuntimeInfo.getHttpPort());

        String fakeMessage = """
                {"id":"testID","time":1762935416, "event":"keepalive","topic":"catChat", "message":"Filtreras bort"}
                {"id":"testID","time":1762935416, "event":"message","topic":"catChat", "message":"User: Hej"}
                """;


        //Simulerar en server
        stubFor(get(urlEqualTo("/catChat/json")).willReturn(aResponse()
                .withStatus(200)
                .withBody(fakeMessage)));
        var model = new HelloModel(host);

        //Act

        model.receiveMessage();

        Thread.sleep(1000);


        //Assert
        assertThat(model.getMessages().getFirst().message()).isEqualTo("User: Hej");

        model.stopSubscription(); //Stänger anslutningen
    }

    /**
     * Verifies that calling sendMessage() sends a correct HTTP POST request
     * to the simulated server, including the message content in the request body.
     *
     * @param wmRuntimeInfo Provides port information for the simulated server.
     */
    @Test
    void sendMessageToFakeServer(WireMockRuntimeInfo wmRuntimeInfo) {
        //Arrange
        var con = new NtfyConnectionImpl("http://localhost:" + wmRuntimeInfo.getHttpPort());
        var model = new HelloModel(con);
        model.setMessageToSend("");
        stubFor(post("/catChat").willReturn(ok()));

        //Act
        model.sendMessage("Hello World");

        //Assert
        WireMock.verify(postRequestedFor(urlEqualTo("/catChat"))
                .withRequestBody(matching("Hello World")));
    }

    /**
     * Tests that a simulated incoming message (from the Spy) is correctly added
     * to the ObservableList held by the model.
     * This verifies the model's message handling flow.
     */
    @Test
    void messageIsAddedToObservableList() {
        //Arrange
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);

        //Act
        model.receiveMessage();
        var testText = new NtfyMessageDto("id1", 15465823L, "Message", "catChat", "Godmorgon");
        spy.simulateIncomingMessage(testText);

        //Assert
        assertThat(model.getMessages()).extracting(NtfyMessageDto::message).contains("Godmorgon");
    }

    /**
     * Verifies that calling sendFile() sends a correct HTTP POST request
     * to the simulated server, including the message content as the 'Title' header
     * and the file's content in the request body.
     *
     * @param wmRuntimeInfo Provides port information for the simulated server.
     */
    @Test
    void sendFileToFakeServer(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        //Arrange
        var con = new NtfyConnectionImpl("http://localhost:" + wmRuntimeInfo.getHttpPort());
        var model = new HelloModel(con);
        Path fakeFile = Files.createTempFile("fakeImage", ".png");
        Files.writeString(fakeFile, "TestFile");
        stubFor(post("/catChat").willReturn(ok()));

        //Act
        model.sendFile(fakeFile, "TestFile");

        //Assert
        WireMock.verify(postRequestedFor(urlEqualTo("/catChat"))
                .withRequestBody(containing("TestFile")));
    }
}