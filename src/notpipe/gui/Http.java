package notpipe.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public final class Http {
    private Http() {}

    public static String get(String url) throws IOException {
        System.out.println("HTTP " + url);
        URL u = new URL(url);
        HttpURLConnection c = (HttpURLConnection)u.openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(18000);
        c.setRequestProperty("User-Agent", "NOS-Pipe/0.2");
        c.setInstanceFollowRedirects(true);

        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        if (in == null) throw new IOException("HTTP " + code + " " + url);
        try {
            String text = read(in);
            if (code >= 400) throw new IOException("HTTP " + code + " " + url);
            return text;
        } finally {
            try { in.close(); } catch (Exception ignored) {}
            c.disconnect();
        }
    }

    /** Checks that the media server accepts the URL without downloading the video. */
    public static int probeMedia(String url) throws IOException {
        URL u = new URL(url);
        HttpURLConnection c = (HttpURLConnection)u.openConnection();
        c.setConnectTimeout(5000);
        c.setReadTimeout(5000);
        c.setRequestProperty("User-Agent", "MPlayer");
        c.setRequestProperty("Range", "bytes=0-1");
        c.setInstanceFollowRedirects(true);
        try {
            return c.getResponseCode();
        } finally {
            c.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuffer b = new StringBuffer();
        String line;
        while ((line = r.readLine()) != null) b.append(line).append('\n');
        return b.toString();
    }

    public static String enc(String s) throws IOException {
        return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
    }
}
