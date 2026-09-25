package notpipe.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public final class GatewayConfig {
    private GatewayConfig() {}

    public static UiConfig loadUiConfig(File file) throws IOException {
        return UiConfig.load(file);
    }

    public static Gateway[] load(File file) throws IOException {
        Properties p = new Properties();
        InputStream in = new FileInputStream(file);
        try {
            p.load(in);
        } finally {
            in.close();
        }

        List list = new ArrayList();
        for (int i = 1; i <= 20; i++) {
            String n = pad(i);
            String enabled = p.getProperty("GATEWAY_" + n + "_ENABLED");
            String api = p.getProperty("GATEWAY_" + n + "_API");
            if (enabled == null || api == null || api.trim().length() == 0) continue;

            boolean on = "true".equalsIgnoreCase(enabled.trim());
            if (!on) continue;

            int priority = parseInt(p.getProperty("GATEWAY_" + n + "_PRIORITY"), 1000 + i);
            String type = value(p.getProperty("GATEWAY_" + n + "_TYPE"), "INVIDIOUS");
            String proxy = value(p.getProperty("GATEWAY_" + n + "_PROXY"), "");
            String name = value(p.getProperty("GATEWAY_" + n + "_NAME"), "Gateway " + i);

            list.add(new Gateway(name, type.toUpperCase(), trimSlash(api),
                    trimSlash(proxy), priority, true));
        }

        Collections.sort(list, new Comparator() {
            public int compare(Object a, Object b) {
                Gateway ga = (Gateway)a;
                Gateway gb = (Gateway)b;
                if (ga.priority < gb.priority) return -1;
                if (ga.priority > gb.priority) return 1;
                return ga.name.compareToIgnoreCase(gb.name);
            }
        });

        Gateway[] out = new Gateway[list.size()];
        list.toArray(out);
        return out;
    }

    private static String pad(int i) { return i < 10 ? "0" + i : "" + i; }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static String value(String s, String def) { return s == null ? def : s.trim(); }

    private static String trimSlash(String s) {
        while (s.endsWith("/") && s.length() > 0) s = s.substring(0, s.length() - 1);
        return s;
    }
}
