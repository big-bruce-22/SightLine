package ai.speechtotext.transcription;

public record LineTranscription(String text, String startTime, String endTime) {
    
    public static LineTranscription END = new LineTranscription(null, null, null);

    public String toString() {
        return "[%s -> %s] %s".formatted(startTime, endTime, text);
    }

    public static LineTranscription fromString(String line) {
        // Expected format: "[startTime -> endTime] text"
        int timeEndIndex = line.indexOf("] ");
        if (line.startsWith("[") && timeEndIndex != -1) {
            String timePart = line.substring(1, timeEndIndex);
            String[] times = timePart.split(" -> ");
            if (times.length == 2) {
                String startTime = times[0].trim();
                String endTime = times[1].trim();
                String text = line.substring(timeEndIndex + 2).trim();
                return new LineTranscription(text, startTime, endTime);
            }
        }
        throw new IllegalArgumentException("Invalid transcription line format: " + line);
    }
}
