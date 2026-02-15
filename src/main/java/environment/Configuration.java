package environment;

import java.nio.file.Paths;

public class Configuration {

    // public static String sessionsSavePath = System.getProperty("user.dir") +  "/sessions";
    public static String sessionsSavePath = Paths.get("").toAbsolutePath() + "/live-captioning-system/sessions";
}
