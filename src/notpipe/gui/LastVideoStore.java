package notpipe.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

/** Stores exactly one last-launched YouTube video ID in gateways.properties. */
public final class LastVideoStore {
    private static final String KEY = "last.video.id";

    private LastVideoStore() {}

    public static synchronized String load(File file) {
        try {
            Properties p = read(file);
            String id = p.getProperty(KEY, "").trim();
            return id.length() <= 64 ? id : "";
        } catch (Exception e) {
            Log.info("Cannot read last video ID: " + e);
            return "";
        }
    }

    public static synchronized void save(File file, String id) {
        if (id == null || id.trim().length() == 0) return;
        try {
            Properties p = read(file);
            p.setProperty(KEY, id.trim());
            FileOutputStream out = new FileOutputStream(file);
            try {
                p.store(out, "NOS-Pipe configuration");
            } finally {
                out.close();
            }
            Log.info("Last video ID updated: " + id.trim());
        } catch (Exception e) {
            Log.info("Cannot save last video ID: " + e);
        }
    }

    private static Properties read(File file) throws Exception {
        Properties p = new Properties();
        InputStream in = new FileInputStream(file);
        try {
            p.load(in);
        } finally {
            in.close();
        }
        return p;
    }
}
