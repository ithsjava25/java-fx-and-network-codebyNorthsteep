package com.example;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

public class NtfyConnectionImpl implements NtfyConnection {
    //Adressen till servern
    private final String hostName;
    //För att skicka och ta emot HTTP-meddelanden
    private final HttpClient http = HttpClient.newHttpClient();
    //För att konvertera JSON till java objekt
    private final ObjectMapper mapper = new ObjectMapper();

    public NtfyConnectionImpl() {
        Dotenv dotenv = Dotenv.load();
        hostName = Objects.requireNonNull(dotenv.get("HOST_NAME"));
    }

    public NtfyConnectionImpl(String hostName) {
        this.hostName = hostName;
    }

    //Skickar ett meddelande till servern via POST
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


    //Skapar en asynkron (flera trådar) GET-stream
    //VArje rad ändras till ett NtfyMessageDTO och skickas till messageHandler, hopp in i model
    //Returnerar ett objekt av Subscription som kan stoppa streamen(connected.cancel(true))
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
                        .forEach(message -> runOnFx(()-> messageHandler.accept(message))));
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
    private static void runOnFx(Runnable task) {
        try {
            if (Platform.isFxApplicationThread()) task.run();
            else Platform.runLater(task);
        } catch (IllegalStateException notInitialized) {
            // JavaFX toolkit not initialized (e.g., unit tests): run inline
            task.run();
        }
    }
}
