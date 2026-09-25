package notpipe.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Locale;
import java.util.Vector;

/**
 * Tiny loopback HTTP adapter for old Mac MPlayer builds which reject modern
 * upstream 206 responses. The first player request sees a short HTTP/1.0 200
 * URL. Later byte-range requests are relayed as simple HTTP/1.0 206 replies,
 * allowing the MOV demuxer to find an MP4 index and seek without a temp file.
 */
public final class LegacyHttpBridge {
    private final String upstreamUrl;
    private final ServerSocket server;
    private volatile boolean stopped;
    private final Vector activeClients = new Vector();
    private final Vector activeUpstreams = new Vector();

    private LegacyHttpBridge(String upstreamUrl) throws IOException {
        this.upstreamUrl = upstreamUrl;
        server = new ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"));
    }

    public static LegacyHttpBridge start(String upstreamUrl) throws IOException {
        final LegacyHttpBridge bridge = new LegacyHttpBridge(upstreamUrl);
        Thread thread = new Thread(new Runnable() {
            public void run() { bridge.serve(); }
        }, "NOS-Pipe-legacy-http-bridge");
        thread.setDaemon(true);
        thread.start();
        Log.info("Legacy HTTP bridge started on 127.0.0.1:" + bridge.server.getLocalPort());
        return bridge;
    }

    public String localUrl() {
        return "http://127.0.0.1:" + server.getLocalPort() + "/video.mp4";
    }

    public void stop() {
        stopped = true;
        try { server.close(); } catch (Exception ignored) { }
        synchronized (activeClients) {
            for (int i = 0; i < activeClients.size(); i++) {
                try { ((Socket)activeClients.elementAt(i)).close(); }
                catch (Exception ignored) { }
            }
            activeClients.removeAllElements();
        }
        synchronized (activeUpstreams) {
            for (int i = 0; i < activeUpstreams.size(); i++) {
                try { ((HttpURLConnection)activeUpstreams.elementAt(i)).disconnect(); }
                catch (Exception ignored) { }
            }
            activeUpstreams.removeAllElements();
        }
    }

    private void serve() {
        try {
            while (!stopped) {
                final Socket client = server.accept();
                Thread handler = new Thread(new Runnable() {
                    public void run() { serveOne(client); }
                }, "NOS-Pipe-legacy-http-client");
                handler.setDaemon(true);
                handler.start();
            }
        } catch (IOException e) {
            if (!stopped) Log.info("Legacy HTTP bridge stopped: " + e);
        } finally {
            stop();
        }
    }

    private void serveOne(Socket client) {
        HttpURLConnection upstream = null;
        InputStream media = null;
        OutputStream out = null;
        try {
            activeClients.addElement(client);
            client.setSoTimeout(15000);
            BufferedReader request = new BufferedReader(new InputStreamReader(
                    client.getInputStream(), "ISO-8859-1"));
            String first = request.readLine();
            if (first == null) return;
            boolean headOnly = first.startsWith("HEAD ");
            String range = null;
            String line;
            while ((line = request.readLine()) != null && line.length() > 0) {
                int colon = line.indexOf(':');
                if (colon > 0 && "range".equals(line.substring(0, colon).trim()
                        .toLowerCase(Locale.US))) {
                    String value = line.substring(colon + 1).trim();
                    if (isByteRange(value)) range = value;
                }
            }

            upstream = (HttpURLConnection)new URL(upstreamUrl).openConnection();
            activeUpstreams.addElement(upstream);
            upstream.setConnectTimeout(12000);
            upstream.setReadTimeout(30000);
            upstream.setRequestProperty("User-Agent", "NOS-Pipe/1.0.2");
            upstream.setRequestProperty("Accept", "*/*");
            if (range != null) upstream.setRequestProperty("Range", range);
            upstream.setUseCaches(false);
            int code = upstream.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK
                    && code != HttpURLConnection.HTTP_PARTIAL) {
                throw new IOException("upstream HTTP " + code);
            }

            media = upstream.getInputStream();
            out = client.getOutputStream();
            String type = upstream.getContentType();
            if (type == null || type.length() == 0) type = "video/mp4";
            long length = headerLong(upstream, "Content-Length");
            String contentRange = upstream.getHeaderField("Content-Range");
            long requestedStart = rangeStart(range);

            // A few proxies ignore Range. Preserve compatibility by discarding
            // bytes locally and still presenting a valid partial response.
            if (range != null && code == HttpURLConnection.HTTP_OK
                    && requestedStart > 0) {
                skipFully(media, requestedStart);
                if (length >= 0) {
                    long total = length;
                    length = total - requestedStart;
                    contentRange = "bytes " + requestedStart + "-"
                            + (total - 1) + "/" + total;
                }
            }

            boolean partial = range != null;
            StringBuffer headers = new StringBuffer();
            headers.append(partial ? "HTTP/1.0 206 Partial Content\r\n"
                    : "HTTP/1.0 200 OK\r\n");
            headers.append("Content-Type: ").append(type).append("\r\n");
            if (length >= 0) headers.append("Content-Length: ").append(length).append("\r\n");
            if (partial && contentRange != null && contentRange.length() > 0) {
                headers.append("Content-Range: ").append(contentRange).append("\r\n");
            }
            headers.append("Accept-Ranges: bytes\r\n");
            headers.append("Connection: close\r\n\r\n");
            out.write(headers.toString().getBytes("ISO-8859-1"));
            out.flush();
            Log.info("Legacy HTTP bridge request: upstream HTTP " + code
                    + (range != null ? "; range=" + range : "; full")
                    + (length >= 0 ? "; bytes=" + length : ""));

            if (!headOnly) {
                byte[] buffer = new byte[32768];
                int count;
                while (!stopped && (count = media.read(buffer)) >= 0) {
                    if (count > 0) out.write(buffer, 0, count);
                }
            }
            out.flush();
        } catch (Exception e) {
            if (!stopped) Log.info("Legacy HTTP bridge request failed: " + e);
            try {
                if (out == null) out = client.getOutputStream();
                out.write(("HTTP/1.0 502 Bad Gateway\r\nConnection: close\r\n\r\n")
                        .getBytes("ISO-8859-1"));
                out.flush();
            } catch (Exception ignored) { }
        } finally {
            try { if (media != null) media.close(); } catch (Exception ignored) { }
            if (upstream != null) upstream.disconnect();
            try { if (out != null) out.close(); } catch (Exception ignored) { }
            try { client.close(); } catch (Exception ignored) { }
            activeClients.removeElement(client);
            if (upstream != null) activeUpstreams.removeElement(upstream);
        }
    }

    private static boolean isByteRange(String value) {
        if (value == null || !value.toLowerCase(Locale.US).startsWith("bytes=")) return false;
        // Old MPlayer uses a single range. Multiple ranges require multipart
        // output and are deliberately rejected here.
        return value.indexOf(',') < 0 && value.indexOf('-') > 6;
    }

    private static long rangeStart(String range) {
        if (range == null) return -1;
        int equals = range.indexOf('=');
        int dash = range.indexOf('-', equals + 1);
        if (equals < 0 || dash <= equals + 1) return -1;
        try {
            return Long.parseLong(range.substring(equals + 1, dash).trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static long headerLong(HttpURLConnection connection, String name) {
        String value = connection.getHeaderField(name);
        if (value == null) return -1;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static void skipFully(InputStream in, long amount) throws IOException {
        long remaining = amount;
        byte[] discard = new byte[32768];
        while (remaining > 0) {
            long skipped = in.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
                continue;
            }
            int count = in.read(discard, 0, (int)Math.min((long)discard.length, remaining));
            if (count < 0) throw new IOException("upstream ended before requested range");
            remaining -= count;
        }
    }
}
