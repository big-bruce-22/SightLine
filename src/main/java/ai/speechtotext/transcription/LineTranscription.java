package ai.speechtotext.transcription;

public record LineTranscription(String text, String startTime, String endTime) {
    
    public static LineTranscription END = new LineTranscription(null, null, null);

    public String toString() {
        return "[%s -> %s] %s".formatted(startTime, endTime, text);
    }
}
