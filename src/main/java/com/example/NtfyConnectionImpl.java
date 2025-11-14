package com.example;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Implements the NtfyConnection interface using the HttpClient.
 * This class handles all HTTP communication for sending (POST/PUT) and
 * receiving (asynchronous GET stream) messages/files with the Ntfy server.
 */
public class NtfyConnectionImpl implements NtfyConnection {
    //Adressen till servern
    private final String hostName;
    //För att skicka och ta emot HTTP-meddelanden
    private final HttpClient http = HttpClient.newHttpClient();
    //För att konvertera JSON till java objekt
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Default constructor that loads the host name from the environment variables (via Dotenv).
     */
    public NtfyConnectionImpl() {
        Dotenv dotenv = Dotenv.load();
        hostName = Objects.requireNonNull(dotenv.get("HOST_NAME"));
    }

    /**
     * Constructor allowing the host name to be explicitly specified, typically used for testing
     * with tools like WireMock.
     *
     * @param hostName The base URL of the Ntfy server.
     */
    public NtfyConnectionImpl(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Sends a plain text message to the server via an HTTP POST request.
     * The message is sent to the 'catChat' topic.
     *
     * @param message The text message content to send.
     * @return true if the request was sent without an immediate I/O error, false otherwise.
     */
    @Override
    public boolean send(String message) {
        //Send message to client - HTTP meddelande
        String inputMessage = Objects.requireNonNull(message);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(inputMessage))
                .header("Cache", "no")
                .uri(URI.create(hostName + "/catChat"))
                .build();
        try {
            var response = http.send(httpRequest, HttpResponse.BodyHandlers.discarding());
            return true;
        } catch (IOException e) {
            System.out.println("Error sending message");
        } catch (InterruptedException e) {
            System.out.println("Sending message interrupted");
        }
        return false;
    }


    /**
     * Creates an asynchronous (multi-threaded) GET stream to receive messages from the server.
     * Each line received is converted to an NtfyMessageDto and passed to the message handler.
     *
     * @param messageHandler The consumer that processes each valid incoming message (i.e., adds it to the model).
     * @return A Subscription object that can be used to stop the stream (cancel the connection).
     */
    @Override
    public Subscription receive(Consumer<NtfyMessageDto> messageHandler) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(hostName + "/catChat/json"))
                .build();

        var connected = http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> response.body()
                        .map(s -> mapper.readValue(s, NtfyMessageDto.class))
                        .filter(message -> message.event().equals("message"))
                        .peek(System.out::println)
                        .forEach(message -> runOnFx(() -> messageHandler.accept(message))));
        return new Subscription() {
            @Override
            public void close() {
                connected.cancel(true);
            }

            @Override
            public boolean isOpen() {
                return !connected.isDone();
            }
        };
    }

    /**
     * Helper method to ensure that a task is executed safely on the JavaFX Application Thread.
     * If the current thread is the FX thread, the task runs immediately; otherwise, it is queued via Platform.runLater.
     * This handles IllegalStateException during unit testing
     *
     * @param task The Runnable task to execute.
     */
    private static void runOnFx(Runnable task) {
        try {
            if (Platform.isFxApplicationThread()) task.run();
            else Platform.runLater(task);
        } catch (IllegalStateException notInitialized) {

            task.run();
        }
    }
}
