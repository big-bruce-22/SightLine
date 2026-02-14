package stt.vosk;

import java.io.IOException;

import org.vosk.Model;

public class Models {

    public static Model tl;
    public static Model en;

    static {
        String base = System.getProperty("user.dir");
        System.out.println("base: " + base);
        try {
            en = new Model(base + "/live-captioning-system/models/en-us-small");
            tl = new Model(base + "/live-captioning-system/models/tl");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
