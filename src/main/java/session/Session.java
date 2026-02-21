package session;

import java.nio.file.Path;

public record Session(String name, Path path) {
    
    public String toString() {
        return "Session " + name + " at " + path.toString(); 
    }
}
