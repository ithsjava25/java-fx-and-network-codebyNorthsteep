package com.example;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;


/**
 * Model layer: encapsulates application data and business logic.
 * Manages message state and handles network communication through the NtfyConnection.
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

    /**
     * Constructs the HelloModel with a specific network connection handler.
     *
     * @param connection The network connection implementation, either a test spy or a real implementation.
     */
    public HelloModel(NtfyConnection connection) {

        this.connection = connection;

    }

    //getter från private, används av controller för att koppla til ListView
    /**
     * Gets the list of messages. Used by the controller to link to the ListView.
     *
     * @return The ObservableList of messages.
     */
    public ObservableList<NtfyMessageDto> getMessages() {
        return messages;
    }

    /**
     * Gets the current value of the message to be sent.
     *
     * @return The current message string.
     */
    public String getMessageToSend() {
        return messageToSend.get();
    }
    /**
     * Returns the StringProperty for the message to be sent.
     *
     * @return The message property.
     */
    public StringProperty messageToSendProperty() {
        return messageToSend;
    }

    /**
     * Sets the message to be sent. (Used primarily for testing).
     *
     * @param message The message string to set.
     */
    public void setMessageToSend(String message) {
        messageToSend.set(message);
    }

    /**
     * Sets the message property and immediately sends the message via the network connection.
     *
     * @param message The message content to be sent.
     */
    public void sendMessage(String message) {

        //För test via spy
        messageToSend.set(message);
        //Riktig chat
        connection.send(messageToSend.get());

    }

    //Startar en prenumeration på inkommande meddelanden,
    //Returnerar ett Subscription-objekt så den kan stoppas
    /**
     * Starts a subscription for incoming messages from the network.
     * If a subscription is already active and open, it returns the existing one.
     *
     * @return The active Subscription object.
     */
    public Subscription receiveMessage() {
if(subscription != null && subscription.isOpen()) {
    return subscription;
}
    return subscription = connection.receive(messages::add);


    }

    /**
     * Stops the active message subscription if it is currently open.
     *
     * @throws IOException If an I/O error occurs during the closing of the subscription.
     */
    public void stopSubscription() throws IOException {
        if (subscription != null && subscription.isOpen())
            subscription.close();

    }
}


