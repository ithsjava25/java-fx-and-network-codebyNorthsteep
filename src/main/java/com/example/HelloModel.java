package com.example;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;


/**
 * Model layer: encapsulates application data and business logic.
 */
public class HelloModel {
    /**
     * Handles and returns a list of messages observed by JavaFX
     * Stores, changes and returns data.
     */

    //Lista som håller alla meddelanden
    //FXCollections.observableArrayList() = Nyckel som gör listan ändrings-bar och uppdaterar GUIt
    private final ObservableList<NtfyMessageDto> messages = FXCollections.observableArrayList();

//Kopplar upp till nätverket , används för att skicka och ta emot meddelanden
    private final NtfyConnection connection;
    //Innehåller meddelandet som ska skickas, kopplat till GUI via SimpleStringProperty
    private final StringProperty messageToSend = new SimpleStringProperty();
    //Fält för att kunna styra anslutningen
    private Subscription subscription = null;

    //Konstruktorn tar emot nätverkskoppling, antingen ett test via spy eller en riktig via impl
    public HelloModel(NtfyConnection connection) {

        this.connection = connection;
        //subscription = receiveMessage(); //subscription startar automatiskt när modellen skapas
    }

    //getter från private, används av controller för att koppla til ListView
    public ObservableList<NtfyMessageDto> getMessages() {
        return messages;
    }
    //test
    public String getMessageToSend() {
        return messageToSend.get();
    }
//Getter från private för meddelandet som ska skickas
    public StringProperty messageToSendProperty() {
        return messageToSend;
    }
//Sätter meddelande för tester
    public void setMessageToSend(String message) {
        messageToSend.set(message);
    }

    //Sätter meddelandet till inkommande parameter från test, eller controller (connection skickar till nätverket)
    public void sendMessage(String message) {

        messageToSend.set(message);
        connection.send(messageToSend.get());

    }

    //Startar en prenumeration på inkommande meddelnaden,
    //Returnerar ett Subscription-objekt så den kan stoppas
    public Subscription receiveMessage() {
if(subscription != null && subscription.isOpen()) {
    return this.subscription;
}
    return subscription = connection.receive(messages::add);


    }

    public void stopSubscription() throws IOException {
        if (subscription != null && subscription.isOpen())
            subscription.close();

    }
}


