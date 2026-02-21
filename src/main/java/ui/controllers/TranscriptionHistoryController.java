package ui.controllers;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.lang3.tuple.Pair;

import ai.speechtotext.transcription.LineTranscription;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import session.Session;

public class TranscriptionHistoryController implements Initializable {

    @FXML 
    private StackPane rootPane;

    @FXML 
    private GridPane sessionsViewGridPane, sessionTranscriptionViewGridPane;

    @FXML 
    private VBox sessionButtonsVbox;

    @FXML
    private Label sessionNameLabel;

    @FXML
    private TextArea transcriptionTextArea;

    @FXML
    private MFXButton refreshButton, backButton;

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        setSessionButtons();

        refreshButton.setOnAction(_ -> {
            sessionButtonsVbox.getChildren().clear();
            setSessionButtons();
        });

        backButton.setOnAction(_ -> {
            transcriptionTextArea.clear();
            sessionNameLabel.setText("");
            sessionsViewGridPane.toFront();
        });

        sessionsViewGridPane.toFront();
    }

    private void setSessionButtons() {
        for (var sessionButton : getSessionsButton()) {
            var button = sessionButton.getLeft();
            var session = sessionButton.getRight();

            button.setId("session-button");
            button.setOnAction(_ -> {
                sessionTranscriptionViewGridPane.toFront();
                sessionNameLabel.setText(session.name());

                try {
                    var transcriptionPath = session.path().resolve("transcription.txt");
                    var trascriptions = Files.readAllLines(transcriptionPath).stream()
                        .map(LineTranscription::fromString)
                        .toList();

                    for (var transcription : trascriptions) {
                        transcriptionTextArea.appendText(transcription.text() + "\n");
                    }
                } catch (NoSuchFileException _) {
                    // If the file doesn't exist, it means that the session was recorded without any transcriptions, so we just show an empty transcription view
                } catch (IOException e) {
                    e.printStackTrace();
                } 
            });
            sessionButtonsVbox.getChildren().add(button);
        }
    }

    private List<Pair<MFXButton, Session>> getSessionsButton() {
        return getSessions().stream().map(session -> 
            Pair.of(new MFXButton(session.name()), session)
        ).toList();
    }

    private List<Session> getSessions() {
        try {
            return Files.list(Path.of(System.getProperty("user.dir") + "/live-captioning-system/sessions/"))
                .map(path -> {
                    String fileName = path.getFileName().toString()
                        .replace("session_", "");
                    String[] dateTime = fileName.split("_");
                    String sessionName = "Date: " + dateTime[0] + " Time: " + dateTime[1].replace("-", ":");
                    return new Session(sessionName, path);
                })
                .toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
