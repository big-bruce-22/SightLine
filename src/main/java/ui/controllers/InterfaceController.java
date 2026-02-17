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

import ai.speechtotext.transcription.Transcriber;
import ai.speechtotext.transcription.LineTranscription;
import ai.speechtotext.transcription.TranscriptionChannel;
import ai.speechtotext.vosk.Models;

import environment.Configuration;

import io.github.palexdev.materialfx.controls.MFXButton;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Duration;

import ui.controllers.Builder.DialogType;

public class InterfaceController implements Initializable {

    static enum InputMethod {
        MICROPHONE, AUDIO_FILE
    }

    private final Stage stage;

    @FXML
    private GridPane rootPane;

    @FXML
    private MFXButton startButton, pauseResumeButton, stopButton;

    @FXML
    private Label progressLabel;

    @FXML
    private MFXButton upButton, downButton;

    @FXML
    private TextArea textArea;

    private Timeline dotsTimeline;
    
    private Transcriber transcriber;

    private final int sampleRate;
    private final AudioFormat format;
    private final DataLine.Info info;
    private TargetDataLine microphone;

    private TranscriptionChannel<LineTranscription> transcriptionChannel = new TranscriptionChannel<>();

    private InputMethod method = InputMethod.AUDIO_FILE;

    private final boolean debugMode = true;

    public InterfaceController(Stage stage) {
        this.stage = stage;

        sampleRate = 16000;
        format = new AudioFormat(sampleRate, 16, 1, true, false);
        info = new DataLine.Info(TargetDataLine.class, format);
        try {
            microphone = (TargetDataLine) AudioSystem.getLine(info);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        upButton.setOnAction(this::handleUpButton);
        downButton.setOnAction(this::handleDownButton);

        startButton.setOnAction(this::startAction);
        pauseResumeButton.setOnAction(this::pauseResumeAction);
        stopButton.setOnAction(this::stopAction);

        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setFocusTraversable(false);

        transcriptionChannel.subscribe(t -> {
            if (t == LineTranscription.END) {
                dotsTimeline.stop();

                Platform.runLater(() -> {
                    progressLabel.setText("Done Transcribing");

                    if (debugMode) {
                        method = null;
                    }

                    transcriber = null;
    
                    startButton.setDisable(false);
                    pauseResumeButton.setDisable(true);
                    stopButton.setDisable(true);
                });
            
                return;
            }
            
            Platform.runLater(() -> {
                if (textArea.getText().isEmpty()) {
                    textArea.appendText(t.text());
                } else {
                    textArea.appendText("\n" + t.text());
                }
            });
        });

        startButton.setDisable(true);
        pauseResumeButton.setDisable(true);
        stopButton.setDisable(true);

        setInputMethod(method);
    }

    private void handleUpButton(ActionEvent event) {
        textArea.fireEvent(new KeyEvent(
            KeyEvent.KEY_PRESSED,
            "",
            "",
            KeyCode.UP,
            false, false, false, false
        ));
    }

    private void handleDownButton(ActionEvent event) {
        textArea.fireEvent(new KeyEvent(
            KeyEvent.KEY_PRESSED,
            "",
            "",
            KeyCode.DOWN,
            false, false, false, false
        ));
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

        if (debugMode) {
            transcriber = null;
        } 
        
        startButton.setDisable(false);
        pauseResumeButton.setDisable(true);
        stopButton.setDisable(true);
    }

    private void pauseResumeAction(ActionEvent event) {
        if (transcriber == null) return;

        if (pauseResumeButton.getText().equals("Pause")) {
            transcriber.pause();
            pauseResumeButton.setText("Resume");
            progressLabel.setText("Paused");
            dotsTimeline.stop();
        } else {
            transcriber.resume();
            pauseResumeButton.setText("Pause");
            dotsTimeline.play();
        }
    }

    private void startAction(ActionEvent event) {
        if (method == null && debugMode) {
            int[] pressed = new int[1];
            Builder.newChoiceDialog(
                stage,
                rootPane,
                "Input method", 
                "Input method", 
                "Select input method", 
                null, 
                new String[] {"Microphone", "Audio File"}, 
                pressed, 
                null
            ).showAndWait();

            method = switch (pressed[0]) {
                case 0 -> InputMethod.MICROPHONE;
                case 1 -> InputMethod.AUDIO_FILE;
                default -> null;
            };

            setInputMethod(method);

            startButton.setDisable(true);
        }

        dotsTimeline = new Timeline(
            new KeyFrame(Duration.seconds(0.0), _ -> progressLabel.setText("Transcribing")),
            new KeyFrame(Duration.seconds(0.5), _ -> progressLabel.setText("Transcribing.")),
            new KeyFrame(Duration.seconds(1.0), _ -> progressLabel.setText("Transcribing..")),
            new KeyFrame(Duration.seconds(1.5), _ -> progressLabel.setText("Transcribing...")),
            new KeyFrame(Duration.seconds(2.0), _ -> progressLabel.setText("Transcribing..."))
        );

        dotsTimeline.setCycleCount(Animation.INDEFINITE);
        dotsTimeline.play();

        new Thread(() -> {
            switch (method) {
                case MICROPHONE -> {
                    startButton.setDisable(true);
                    pauseResumeButton.setDisable(false);
                    stopButton.setDisable(false);

                    File sessionsFolder = new File(Configuration.sessionsSavePath);

                    if (!sessionsFolder.exists()) {
                        sessionsFolder.mkdirs();
                    }

                    File currentSessionFolder = new File(sessionsFolder, "session_" + System.currentTimeMillis());

                    if (!currentSessionFolder.exists()) {
                        currentSessionFolder.mkdirs();
                    }

                    File sessionTranscriptionFile = new File(currentSessionFolder, "transcription.txt");

                    transcriber.transcribe(sessionTranscriptionFile, transcriptionChannel, true);
                }
                case AUDIO_FILE -> {
                    startButton.setDisable(true);
                    pauseResumeButton.setDisable(false);
                    stopButton.setDisable(false);

                    File sessionsFolder = new File(Configuration.sessionsSavePath);

                    if (!sessionsFolder.exists()) {
                        sessionsFolder.mkdirs();
                    }

                    File currentSessionFolder = new File(sessionsFolder, "session_" + System.currentTimeMillis());

                    if (!currentSessionFolder.exists()) {
                        currentSessionFolder.mkdirs();
                    }

                    File sessionTranscriptionFile = new File(currentSessionFolder, "transcription.txt");

                    transcriber.transcribe(sessionTranscriptionFile, transcriptionChannel, false);
                }
            }
        }).start();
    }

    private void setInputMethod(InputMethod method) {
        System.out.println("Selected input method: " + method);
        if (method == null) return;
        switch (method) {
            case MICROPHONE -> handleMicrophoneInput();
            case AUDIO_FILE -> handleAudioFileInput();
        }
    }

    private void handleMicrophoneInput() {
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() {
                try {
                    var recognizer = new Recognizer(Models.tl, sampleRate);
                    if (!microphone.isOpen()) {
                        microphone.open(format);
                    }
                    
                    microphone.start();
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
        fileChooser.getExtensionFilters().add(new ExtensionFilter("WAV files (*.wav)", "*.wav"));
        fileChooser.setInitialDirectory(
            System.getProperty("user.home") != null
                ? new File(System.getProperty("user.home"))
                : new File(".")
        );
        return fileChooser.showOpenDialog(stage);
    }
}
