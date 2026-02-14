package stt.transcription;

public record Transcription(String text, String startTime, String endTime) {
    
    public static Transcription END = new Transcription(null, null, null);
}
