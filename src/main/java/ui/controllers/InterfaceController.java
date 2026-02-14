package ui.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.vosk.Recognizer;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXScrollPane;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import lombok.RequiredArgsConstructor;

import stt.transcription.Transcriber;
import stt.transcription.Transcription;
import stt.transcription.TranscriptionChannel;
import stt.vosk.Models;
import ui.controllers.Builder.DialogType;

@RequiredArgsConstructor
public class InterfaceController implements Initializable {

    private final Stage stage;

    @FXML
    private GridPane rootPane;

    @FXML
    private MFXButton startButton, pauseResumeButton, stopButton;

    @FXML
    private MFXComboBox<String> inputComboBox;

    @FXML
    private Label progressLabel;

    @FXML
    private VBox textVbox;

    @FXML
    private MFXScrollPane viewScrollPane;

    private Transcriber transcriber;

    private Timeline dotsTimeline;

    private final int sampleRate = 16000;
    private final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
    private final DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

    private TranscriptionChannel<Transcription> transcriptionChannel = new TranscriptionChannel<>();

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        Commons.setClicableComboBox(inputComboBox);

        inputComboBox.getItems().addAll("Microphone", "Audio File");
        inputComboBox.valueProperty().addListener(inputMethodChanged());

        startButton.setOnAction(this::startAction);
        pauseResumeButton.setOnAction(this::pauseResumeAction);
        stopButton.setOnAction(this::stopAction);

        transcriptionChannel.subscribe(t -> {
            if (t == Transcription.END) {
                dotsTimeline.stop();

                Platform.runLater(() -> {
                    progressLabel.setText("Transcription ended");

                    transcriber = null;
    
                    startButton.setDisable(true);
                    pauseResumeButton.setDisable(true);
                    stopButton.setDisable(true);
    
                    inputComboBox.setDisable(false);
                    inputComboBox.clearSelection();
                    inputComboBox.setText("Select Input");
                });
            
                return;
            }

            Platform.runLater(() -> {
                textVbox.getChildren().add(new Text(t.text()));
            });

            viewScrollPane.setVvalue(Double.MAX_VALUE); // auto scroll to bottom
        });

        startButton.setDisable(true);
        pauseResumeButton.setDisable(true);
        stopButton.setDisable(true);
    }

    private void stopAction(ActionEvent event) {
        if (transcriber == null) return;

        Timeline dotsTimeline = new Timeline(
            new KeyFrame(Duration.seconds(0.0), e -> progressLabel.setText("Stopping.")),
            new KeyFrame(Duration.seconds(0.5), e -> progressLabel.setText("Stopping..")),
            new KeyFrame(Duration.seconds(1.0), e -> progressLabel.setText("Stopping...")),
            new KeyFrame(Duration.seconds(1.5), e -> progressLabel.setText("Stopped"))
        );

        dotsTimeline.play();

        this.dotsTimeline.stop();

        transcriber.stop();
        transcriber = null;

        startButton.setDisable(true);
        pauseResumeButton.setDisable(true);
        stopButton.setDisable(true);

        inputComboBox.setDisable(false);
        inputComboBox.clearSelection();
        inputComboBox.setText("Select Input");
    }

    private void pauseResumeAction(ActionEvent event) {
        if (transcriber == null) return;

        if (pauseResumeButton.getText().equals("Pause")) {
            transcriber.pause();
            pauseResumeButton.setText("Resume");
        } else {
            transcriber.resume();
            pauseResumeButton.setText("Pause");
        }
    }

    private void startAction(ActionEvent event) {
        dotsTimeline = new Timeline(
            new KeyFrame(Duration.seconds(0.0), e -> progressLabel.setText("Transcribing")),
            new KeyFrame(Duration.seconds(0.5), e -> progressLabel.setText("Transcribing.")),
            new KeyFrame(Duration.seconds(1.0), e -> progressLabel.setText("Transcribing..")),
            new KeyFrame(Duration.seconds(1.5), e -> progressLabel.setText("Transcribing...")),
            new KeyFrame(Duration.seconds(2.0), e -> progressLabel.setText("Transcribing..."))
        );

        dotsTimeline.setCycleCount(Animation.INDEFINITE);
        dotsTimeline.play();

        inputComboBox.setDisable(true);

        new Thread(() -> {
            switch (inputComboBox.getValue()) {
                case "Microphone" -> {
                    startButton.setDisable(true);
                    pauseResumeButton.setDisable(false);
                    stopButton.setDisable(false);

                    transcriber.transcribe(transcriptionChannel, true);
                }
                case "Audio File" -> {
                    startButton.setDisable(true);
                    pauseResumeButton.setDisable(false);
                    stopButton.setDisable(false);

                    transcriber.transcribe(transcriptionChannel, false);
                }
                default -> throw new IllegalStateException("Unexpected value: " + inputComboBox.getValue());
            }
        }).start();
    }

    private ChangeListener<? super String> inputMethodChanged() {
        return (obs, oldVal, newVal) -> {
            if (newVal == null) return;
            switch (newVal) {
                case "Microphone" -> handleMicrophoneInput();
                case "Audio File" -> handleAudioFileInput();
            }
        };
    }

    private void handleMicrophoneInput() {
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() {
                try {
                    var recognizer = new Recognizer(Models.tl, sampleRate);
                    var microphone = (TargetDataLine) AudioSystem.getLine(info);
                    transcriber = new Transcriber(recognizer, microphone, null);
                } catch (IOException | LineUnavailableException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        playLoadingAnimation(loadTask);
    }

    private void handleAudioFileInput() {
        File file = chooseAudioFile();
        if (file == null) {
            Builder.newDialog(stage, rootPane, "Select a .wav file!", DialogType.ERROR, null)
                .showAndWait();
            
            inputComboBox.getSelectionModel().clearSelection();
            return;
        }

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() {
                try {
                    Recognizer recognizer = new Recognizer(Models.tl, sampleRate); // keep it open
                    AudioInputStream ais = AudioSystem.getAudioInputStream(file);
                    AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
                    AudioInputStream audio = AudioSystem.getAudioInputStream(format, ais);

                    transcriber = new Transcriber(recognizer, null, audio);
                } catch (IOException | UnsupportedAudioFileException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        playLoadingAnimation(loadTask);
    }

    private void playLoadingAnimation(Task<Void> loadTask) {
        Timeline dotsTimeline = new Timeline(
            new KeyFrame(Duration.seconds(0.0), e -> progressLabel.setText("Loading")),
            new KeyFrame(Duration.seconds(0.5), e -> progressLabel.setText("Loading.")),
            new KeyFrame(Duration.seconds(1.0), e -> progressLabel.setText("Loading..")),
            new KeyFrame(Duration.seconds(1.5), e -> progressLabel.setText("Loading...")),
            new KeyFrame(Duration.seconds(2.0), e -> progressLabel.setText("Loading..."))
        );
        dotsTimeline.setCycleCount(Animation.INDEFINITE);
        dotsTimeline.play();

        loadTask.setOnSucceeded(e -> {
            dotsTimeline.stop();
            progressLabel.setText("Ready");
            startButton.setDisable(false);
        });

        loadTask.setOnFailed(e -> {
            dotsTimeline.stop();
            progressLabel.setText("Failed to load");
            startButton.setDisable(true);
        });

        new Thread(loadTask).start();
    }

    private File chooseAudioFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select .wav file to be opened");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("WAV files (*.wav)", "*.wav")
        );
        fileChooser.setInitialDirectory(System.getProperty("user.home") != null
            ? new File(System.getProperty("user.home"))
            : new File("."));
        return fileChooser.showOpenDialog(stage);
    }
}
