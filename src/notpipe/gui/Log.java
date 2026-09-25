package notpipe.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Optional diagnostic log written beside the active configuration. */
public final class Log {
    private static PrintStream logFile;

    private Log() {}

    public static void init() {
        if (!Boolean.getBoolean("nospipe.log")) {
            return;
        }
        try {
            logFile = new PrintStream(new FileOutputStream(AppPaths.diagnosticLogFile(), false), true, "UTF-8");
            System.setOut(new PrintStream(new TeeOutputStream(System.out, logFile), true));
            System.setErr(new PrintStream(new TeeOutputStream(System.err, logFile), true));
            info("NOS-Pipe 1.0.2 session started");
            info("Java " + System.getProperty("java.version") + "; "
                    + System.getProperty("os.name") + " " + System.getProperty("os.version")
                    + "; " + System.getProperty("os.arch"));
        } catch (Exception e) {
            System.err.println("Cannot create NOS-Pipe.log: " + e);
        }
    }

    public static synchronized void info(String text) {
        System.out.println("[" + timestamp() + "] " + text);
    }

    public static File applicationDirectory() throws Exception {
        File location = new File(Log.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        File baseDir = location.isDirectory() ? location : location.getParentFile();
        if (baseDir == null) throw new IOException("NOS-Pipe folder not found");
        return baseDir;
    }

    private static String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }

    private static final class TeeOutputStream extends OutputStream {
        private final OutputStream first;
        private final OutputStream second;

        TeeOutputStream(OutputStream first, OutputStream second) {
            this.first = first;
            this.second = second;
        }

        public synchronized void write(int value) throws IOException {
            first.write(value);
            second.write(value);
        }

        public synchronized void write(byte[] data, int offset, int length) throws IOException {
            first.write(data, offset, length);
            second.write(data, offset, length);
        }

        public synchronized void flush() throws IOException {
            first.flush();
            second.flush();
        }
    }
}
