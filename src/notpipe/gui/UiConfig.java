package notpipe.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** User-adjustable presentation/playback settings stored beside gateways. */
public final class UiConfig {
    public final int fontSize;
    public final int videoHeight;
    public final String playerMode;
    public final String playerCommand;
    public final int windowX;
    public final int windowY;
    public final int windowWidth;
    public final int windowHeight;

    private UiConfig(int fontSize, int videoHeight, String playerMode, String playerCommand,
                     int windowX, int windowY, int windowWidth, int windowHeight) {
        this.fontSize = fontSize;
        this.videoHeight = videoHeight;
        this.playerMode = playerMode;
        this.playerCommand = playerCommand;
        this.windowX = windowX;
        this.windowY = windowY;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
    }

    public static UiConfig load(File file) throws IOException {
        Properties p = new Properties();
        InputStream in = new FileInputStream(file);
        try { p.load(in); } finally { in.close(); }
        int font = parseInt(p.getProperty("font.size"), 14);
        if (font < 8 || font > 24) font = 14;
        int height = parseInt(p.getProperty("video.height"), 360);
        if (height < 144) height = 144;
        String mode = value(p.getProperty("player.mode"), "auto").toLowerCase();
        if (!"auto".equals(mode) && !"mplayer".equals(mode)
                && !"system".equals(mode) && !"custom".equals(mode)) {
            mode = "auto";
        }
        String command = value(p.getProperty("player.command"), "");
        int x = parseInt(p.getProperty("window.x"), -1);
        int y = parseInt(p.getProperty("window.y"), -1);
        int width = parseInt(p.getProperty("window.width"), 640);
        int windowHeight = parseInt(p.getProperty("window.height"), 360);
        return new UiConfig(font, height, mode, command, x, y, width, windowHeight);
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static String value(String s, String def) {
        return s == null ? def : s.trim();
    }
}
