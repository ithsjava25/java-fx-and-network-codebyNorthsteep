package com.example;

import java.util.function.Consumer;

/**
 * Defines the contract for connecting to and interacting with the Ntfy notification server.
 * Implementations are responsible for handling the underlying network communication (HTTP).
 */
public interface NtfyConnection {

    /**
     * Sends a plain text message to the server's designated topic.
     *
     * @param message The text message content to send.
     * @return true if the message was successfully queued for sending (or sent), false otherwise.
     */
    boolean send(String message);

    /**
     * Starts a subscription (stream) to receive incoming messages from the server.
     * The provided consumer will be executed asynchronously whenever a new message arrives.
     *
     * @param consumer The Consumer that processes each received message DTO.
     * @return A Subscription object that can be used to stop the stream connection.
     */
    Subscription receive(Consumer<NtfyMessageDto> consumer);

}
