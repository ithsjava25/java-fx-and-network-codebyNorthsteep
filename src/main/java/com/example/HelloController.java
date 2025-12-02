package com.example;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Controller layer: mediates between the view (FXML) and the model.
 * Handles updates of the chat-window and manages UI,
 * such as sending messages and connecting to server.
 */
public class HelloController {

    //En model skapas som i bakgrunden är en lista och håller koll på meddelanden
    private final HelloModel model = new HelloModel(new NtfyConnectionImpl());

    //@FXML kopplingar

    //Knappar för uppkoppling
    @FXML
    private Button connectToServer;

    @FXML
    private Button disconnectFromServer;

    //Kopplar en knapp från FXML för att skicka filer
    @FXML
    private Button sendFile;

    //Kopplar ett textfält från FXML där användaren skriver ett meddelande
    @FXML
    private TextField messageInput;

    //Kopplar en knapp från FXML som klickas på för att skicka meddelandet
    @FXML
    private Button sendButton;


    //Ytan för alla meddelanden som visas
    @FXML
    private ListView<NtfyMessageDto> chatBox;

    /**
     * Called automatically when the application starts (after FXML elements are injected).
     * Sets up the initial state, attaches listeners to UI elements, and configures
     * how messages should be displayed in the chatBox.
     */
    @FXML
    private void initialize() {

        //Sätter ursprungstillståndet (default) för skicka-knappen
        updateSendButtonState();

        //Lägger till en lyssnare för att uppdatera knappen vid inmatning av text
        messageInput.textProperty().addListener((observable, oldValue, newValue) -> updateSendButtonState());

        //Om användaren trycker på Enter eller klickar med musen -> skicka meddelandet
        messageInput.setOnAction((event) -> sendMessageToModel());
        sendButton.setOnAction(event -> sendMessageToModel());
        sendFile.setOnAction(event -> sendFileToModel());

        disconnectFromServer.setOnAction(event -> {
            try {
                setDisconnectFromServer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        connectToServer.setOnAction(event -> {
            try {
                setConnectToServer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        disconnectFromServer.setDisable(true);
        
        //Styr hur varje meddelande ska visas i chatboxen
        chatBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(NtfyMessageDto item, boolean empty) {
                super.updateItem(item, empty);
                //Kräver en null check då JavaFX återanvänder cellerna
                if (item == null || empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    //Skapar en label med meddelande-texten och sätter en stil från css
                    Label label = new Label(item.message());
                    label.getStyleClass().add("message-bubble");

                    String time = item.formattedTime();
                    Label labelTime = new Label(time);
                    labelTime.getStyleClass().add("time-stamp");

                    //Layout
                    VBox messageBox = new VBox(label, labelTime);
                    messageBox.setSpacing(2);

                    //Vänster eller höger i ListView
                    HBox hbox = new HBox(messageBox);
                    hbox.setMaxWidth(chatBox.getWidth() - 20);

                    String messagePosition = item.message();
                    if (messagePosition != null && messagePosition.startsWith("User:")) {
                        hbox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                        label.getStyleClass().add("outgoing-message");
                    } else {
                        hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        label.getStyleClass().add("incoming-message");
                    }
                    setGraphic(hbox);
                }
            }
        });
        //Kopplar Listan i view med ObservableList i HelloModel
        chatBox.setItems(model.getMessages());
        //Uppdaterar chatBoxen med meddelanden från listan
        model.getMessages().addListener((ListChangeListener<NtfyMessageDto>) changes -> {
            chatBox.refresh();
        });
    }

    /**
     * Reads the text from the input field, sends the message to the model,
     * and then clears the field.
     * The message is sent only if the field is not empty.
     */
    private void sendMessageToModel() {
        String outgoingMessage = messageInput.getText().trim();
        boolean isConnected = !disconnectFromServer.isDisabled();
        //Skicka bara om det finns text och vi är uppkopplade
        if (isConnected && !outgoingMessage.isEmpty()) {
            model.sendMessage("User: " + outgoingMessage);
            //tömmer sedan fältet där text matas in(prompt-meddelande visas igen)
            messageInput.clear();
        }
    }

    /**
     * Opens a file chooser dialog and, if a file is selected, sends the file
     * and a default descriptive message to the model.
     * The operation is executed only if the client is connected to the server.
     */
    private void sendFileToModel() {
        boolean isConnected = !disconnectFromServer.isDisabled();
        FileChooser chooseFile = new FileChooser();
        File file = chooseFile.showOpenDialog(sendFile.getScene().getWindow());
        if (isConnected && file != null) {
            model.sendFile(file.toPath(), "User: You got sent a file");
        }
    }

    /**
     * Updates the state (disable/enable) of the Send button (sendButton).
     * The button is enabled only if:
     * 1. The text field contains text.
     * 2. The application is connected to the server (disconnectFromServer is enabled).
     */
    private void updateSendButtonState() {
        // Kollar om texten, efter att ha tagit bort ledande/efterföljande mellanslag, är tom.
        boolean isTextPresent = !messageInput.getText().trim().isEmpty();

        //Nytt villkor för button för att ej kunna skicka meddelanden till servern om ej connected
        boolean isConnected = !disconnectFromServer.isDisabled();

        // Sätt disable till TRUE om det INTE finns text eller att servern ej är uppkopplad.
        sendButton.setDisable(!isTextPresent || !isConnected);
        // Sätt disable till TRUE om servern ej är uppkopplad.
        sendFile.setDisable(!isConnected);
    }

    /**
     * Starts the subscription to messages via HelloModel (connects to the server).
     * Updates the status of the connection buttons.
     *
     * @throws IOException If an I/O error occurs during connection.
     */
    public void setConnectToServer() throws IOException {
        if (disconnectFromServer.isDisable()) {
            model.receiveMessage();
            connectToServer.setDisable(true);
            disconnectFromServer.setDisable(false);
            updateSendButtonState();

        }
    }

    /**
     * Stops the subscription to messages via HelloModel (disconnects from the server).
     * Updates the status of the connection buttons.
     *
     * @throws IOException If an I/O error occurs during disconnection.
     */
    public void setDisconnectFromServer() throws IOException {
        model.stopSubscription();
        connectToServer.setDisable(false);
        disconnectFromServer.setDisable(true);
        updateSendButtonState();
    }
}