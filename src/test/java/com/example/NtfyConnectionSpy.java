package com.example;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class NtfyConnectionSpy implements NtfyConnection {
    //Meddelande som skickas
    String message;
    //funktionen som ska köras när ett meddelande kommer
    Consumer<NtfyMessageDto> consumer;

    //Sparar meddelandet som ska användas i tester för att kontrollera att rätt sak skickas
    @Override
    public boolean send(String message) {
        this.message = message;
        return false;
    }


    //Sparar en consumer och returnerar en falsk Subscription
    //Sätter consumer till null och returnerar att fake-servern är öppen, detta kan styras via booleanflaggan
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

    //Anropar consumer och simulerar att ett meddelande kom in från nätverket
    public void simulateIncomingMessage(NtfyMessageDto message) {
        if (consumer != null) {
            consumer.accept(message);
        }
    }

    }
