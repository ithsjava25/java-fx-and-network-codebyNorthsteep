package com.example;


import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.function.Consumer;

public interface NtfyConnection {

    //Skicka ett meddelande till servern
    boolean send(String message);

    //Startar en prenumeration och tar emot en consumer som ska köras varje gång ett meddelande kommer
   Subscription receive(Consumer<NtfyMessageDto> consumer);

}
