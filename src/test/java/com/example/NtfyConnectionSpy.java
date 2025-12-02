package com.example;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * A Test Spy implementation of NtfyConnection used for unit testing
 * the HelloModel. This spy records method calls and arguments,
 * and allows simulation of incoming messages.
 */
public class NtfyConnectionSpy implements NtfyConnection {
    //Meddelande som skickas
    String message;
    Path file;
    //funktionen som ska köras när ett meddelande kommer
    Consumer<NtfyMessageDto> consumer;

    /**
     * Records the message content for verification in tests.
     *
     * @param message The message string passed by the model.
     * @return Always returns false or true (return value often ignored in spy tests).
     */
    @Override
    public boolean send(String message) {
        this.message = message;
        return false;
    }


    /**
     * Saves the provided consumer and returns a fake Subscription object.
     * The fake subscription allows tests to control when the connection is closed.
     *
     * @param consumer The consumer function that handles incoming messages.
     * @return A fake Subscription instance.
     */
    @Override
    public Subscription receive(Consumer<NtfyMessageDto> consumer) {
        this.consumer = consumer;

        return new Subscription() {
            private boolean open = true;

            @Override
            public void close() {
                open = false;
                NtfyConnectionSpy.this.consumer = null;
            }

            @Override
            public boolean isOpen() {
                return open;
            }
        };
    }

    /**
     * Simulates sending an outgoing file with a message
     *
     * @param file The file being sent
     * @param message The message to the file
     * @return Always returns false or true (return value often ignored in spy tests).
     */
    @Override
    public boolean sendFile(Path file, String message) {
        this.file = file;
        return false;
    }

    /**
     * Simulates an incoming message from the network by invoking the stored consumer.
     * This is used by tests to trigger message handling logic in the HelloModel.
     *
     * @param message The DTO representing the simulated incoming message.
     */
    public void simulateIncomingMessage(NtfyMessageDto message) {
        if (consumer != null) {
            consumer.accept(message);
        }
    }

}
