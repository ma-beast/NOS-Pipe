package notpipe.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Portable settings stay beside the JAR; protected installs use APPDATA. */
public final class AppPaths {
    private AppPaths() {}

    public static File applicationDirectory() throws Exception {
        return Log.applicationDirectory();
    }

    public static File configFile() throws Exception {
        File template = new File(applicationDirectory(), "gateways.properties");
        if ((!isModernWindows() || !isInsideProgramFiles(applicationDirectory()))
                && (!template.exists() || template.canWrite())) return template;

        File dataDir = userDataDirectory();
        if (!dataDir.exists() && !dataDir.mkdirs() && !dataDir.isDirectory()) {
            throw new IOException("Cannot create settings folder: " + dataDir);
        }
        File userConfig = new File(dataDir, "gateways.properties");
        if (!userConfig.exists()) copy(template, userConfig);
        return userConfig;
    }

    public static File diagnosticLogFile() throws Exception {
        File config = configFile();
        File parent = config.getParentFile();
        if (parent == null) parent = applicationDirectory();
        return new File(parent, "NOS-Pipe.log");
    }

    private static File userDataDirectory() {
        String appData = System.getenv("APPDATA");
        if (appData != null && appData.trim().length() > 0) {
            return new File(appData, "NOS-Pipe");
        }
        return new File(System.getProperty("user.home"), ".nos-pipe");
    }

    private static boolean isModernWindows() {
        String name = System.getProperty("os.name", "").toLowerCase();
        if (name.indexOf("windows") < 0) return false;
        String version = System.getProperty("os.version", "0");
        int dot = version.indexOf('.');
        if (dot >= 0) version = version.substring(0, dot);
        try { return Integer.parseInt(version) >= 6; }
        catch (Exception ignored) { return false; }
    }

    private static boolean isInsideProgramFiles(File directory) {
        String[] names = { "ProgramFiles", "ProgramFiles(x86)", "ProgramW6432" };
        try {
            String app = directory.getCanonicalPath().toLowerCase();
            for (int i = 0; i < names.length; i++) {
                String value = System.getenv(names[i]);
                if (value == null || value.length() == 0) continue;
                String root = new File(value).getCanonicalPath().toLowerCase();
                if (app.equals(root) || app.startsWith(root + File.separator)) return true;
            }
        } catch (IOException ignored) {}
        return false;
    }

    private static void copy(File source, File target) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(target);
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) >= 0) out.write(buffer, 0, count);
        } finally {
            if (in != null) try { in.close(); } catch (IOException ignored) {}
            if (out != null) try { out.close(); } catch (IOException ignored) {}
        }
    }
}
