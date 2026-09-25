package notpipe.gui;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

/** Saves only the main window bounds in the regular NOS-Pipe configuration. */
public final class WindowStateStore {
    private WindowStateStore() {}

    public static synchronized void save(File config, Rectangle bounds) {
        if (config == null || bounds == null || bounds.width < 1 || bounds.height < 1) return;
        try {
            Properties p = new Properties();
            InputStream in = new FileInputStream(config);
            try { p.load(in); } finally { in.close(); }
            p.setProperty("window.x", Integer.toString(bounds.x));
            p.setProperty("window.y", Integer.toString(bounds.y));
            p.setProperty("window.width", Integer.toString(bounds.width));
            p.setProperty("window.height", Integer.toString(bounds.height));
            FileOutputStream out = new FileOutputStream(config);
            try { p.store(out, "NOS-Pipe configuration"); }
            finally { out.close(); }
        } catch (Exception e) {
            Log.info("Cannot save window position: " + e);
        }
    }
}
