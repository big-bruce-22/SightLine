package ai.speechtotext.transcription;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.TargetDataLine;

import org.json.JSONObject;
import org.vosk.Recognizer;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Transcriber {

    @NonNull
    private final Recognizer recognizer;
    
    private final TargetDataLine dataLine;

    private final AudioInputStream audioInputStream;

    private volatile boolean paused = false;
    private volatile boolean stopped = false;

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public void stop() {
        stopped = true;
    }
    
    public void transcribe(File outputTranscriptionFile, TranscriptionChannel<LineTranscription> channel, boolean liveTranscription) {
        if (liveTranscription) {
            if (dataLine == null) {
                throw new IllegalStateException("Data line is not initialized for live transcription.");
            }
            startLiveTranscription(outputTranscriptionFile, channel);
        } else {
            if (audioInputStream == null) {
                throw new IllegalStateException("Audio input stream is not initialized for file transcription.");
            }
            startFileAudioTranscription(outputTranscriptionFile, channel);
        }
    }

    private void startFileAudioTranscription(File transcriptionFile, TranscriptionChannel<LineTranscription> channel) {
        double audioPositionSeconds = 0.0; // running audio timestamp
        int bytesPerSample = 2; // 16-bit audio
        int channels = 1;
        int sampleRate = 16000;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(transcriptionFile))) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = audioInputStream.read(buffer)) >= 0) {
                if (stopped) break;

                if (paused) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }

                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    var recognizedText = new JSONObject(recognizer.getResult());
                    var text = recognizedText.optString("text", "");
                    if (!text.isEmpty()) {
                        text = text.replace(" <unk> ", "");
                        text = text.replace("<unk> ", "");
                        text = text.replace(" <unk>", "");
                        text = text.replace("<unk>", "");
                        
                        // Compute start/end timestamps in seconds
                        double durationSeconds = (double) bytesRead / (sampleRate * bytesPerSample * channels);
                        double startSec = audioPositionSeconds;
                        double endSec = audioPositionSeconds + durationSeconds;

                        LineTranscription transcription = new LineTranscription(
                            text,
                            String.format("%.2f", startSec),
                            String.format("%.2f", endSec)
                        );
                        channel.send(transcription);
                        writer.write(transcription.toString());
                        writer.newLine();
                        
                        audioPositionSeconds += durationSeconds;
                    }
                } else {
                    // Still processing partial audio; increment position anyway
                    double durationSeconds = (double) bytesRead / (sampleRate * bytesPerSample * channels);
                    audioPositionSeconds += durationSeconds;
                }
            }

            channel.send(LineTranscription.END);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startLiveTranscription(File transcriptionFile, TranscriptionChannel<LineTranscription> channel) {
        LocalTime startTime = null;
        LocalTime endTime = null;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(transcriptionFile))) {
            while (true) {
                if (stopped) {
                    break;
                }
        
                if (paused) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                
                byte[] buffer = new byte[4096];
                int bytesRead = dataLine.read(buffer, 0, buffer.length);
    
                if (bytesRead > 0) {
                    if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                        var recognizedText = new JSONObject(recognizer.getResult());
                        var text = recognizedText.optString("text", "");
                        if (!text.isEmpty()) {
                            text = text.replace("<unk> ", "");
                            endTime = LocalTime.now();
                            LineTranscription transcription = new LineTranscription(text, startTime.toString(), endTime.toString());
                            channel.send(transcription);
                            writer.write(transcription.toString());
                            writer.newLine();
    
                            startTime = endTime = null;
                        }
                    } else {
                        if (startTime == null) {
                            startTime = LocalTime.now();
                        }
                    }
                }
            }       
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
