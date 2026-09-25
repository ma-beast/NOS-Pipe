package notpipe.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SearchCoordinator {
    private final Gateway[] gateways;
    private final StatusListener statusListener;

    public SearchCoordinator(Gateway[] gateways) { this(gateways, null); }

    public SearchCoordinator(Gateway[] gateways, StatusListener statusListener) {
        this.gateways = gateways;
        this.statusListener = statusListener;
    }

    public List search(final String query) {
        final VideoResult[][] results = new VideoResult[gateways.length][];
        final String[] errors = new String[gateways.length];
        final int[] progress = new int[] { 0, 0 };
        Thread[] threads = new Thread[gateways.length];

        System.out.println("Launching " + gateways.length + " gateway searches in parallel...");
        status("SEARCH GATEWAYS: 0/" + gateways.length);

        for (int i = 0; i < gateways.length; i++) {
            final int index = i;
            final Gateway g = gateways[i];
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    System.out.println();
                    System.out.println("Searching " + g.type + ": " + g.api);
                    try {
                        results[index] = searchGateway(g, query);
                    } catch (Exception e) {
                        errors[index] = e.toString();
                        System.out.println("  FAILED: " + e.toString());
                    } finally {
                        synchronized (progress) {
                            progress[0]++;
                            if (results[index] != null) progress[1] += results[index].length;
                            status("SEARCH GATEWAYS: " + progress[0] + "/" + gateways.length
                                    + "  FOUND: " + progress[1]);
                        }
                    }
                }
            }, "NOSPipe-Search-" + i);
            threads[i].setDaemon(true);
            threads[i].start();
        }

        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        ArrayList all = new ArrayList();
        Set ids = new HashSet();

        for (int i = 0; i < gateways.length; i++) {
            VideoResult[] got = results[i];
            if (got == null) continue;
            Gateway g = gateways[i];
            int added = 0;
            for (int j = 0; j < got.length; j++) {
                VideoResult v = got[j];
                if (v.videoId.length() == 0 || ids.contains(v.videoId)) continue;
                ids.add(v.videoId);
                all.add(new VideoResult(all.size() + 1, v.title, v.author,
                        v.backend, v.videoId, g, v.durationSeconds));
                added++;
            }
            System.out.println(" " + g.name + " unique added: " + added);
        }
        return all;
    }

    private void status(String text) {
        if (statusListener != null) statusListener.status(text);
    }

    private VideoResult[] searchGateway(Gateway g, String query) throws Exception {
        if ("PIPED".equals(g.type)) return searchPiped(g, query);
        return searchInvidious(g, query);
    }

    private VideoResult[] searchPiped(Gateway g, String query) throws Exception {
        String url = g.api + "/search?filter=videos&q=" + Http.enc(query);
        String body = Http.get(url);
        String[] objs = Json.objects(body);
        ArrayList out = new ArrayList();
        for (int i = 0; i < objs.length; i++) {
            String o = objs[i];
            String id = Json.string(o, "url");
            if (id.indexOf("/watch?v=") >= 0) id = id.substring(id.indexOf("/watch?v=") + 9);
            if (id.indexOf("&") >= 0) id = id.substring(0, id.indexOf("&"));
            if (id.length() == 0) id = Json.string(o, "id");
            String title = Json.string(o, "title");
            String uploader = Json.string(o, "uploaderName");
            int duration = Json.integer(o, "duration");
            if (id.length() > 0 && title.length() > 0)
                out.add(new VideoResult(0, title, uploader, g.type, id, g, duration));
        }
        VideoResult[] a = new VideoResult[out.size()];
        out.toArray(a);
        return a;
    }

    private VideoResult[] searchInvidious(Gateway g, String query) throws Exception {
        String url = g.api + "/api/v1/search?q=" + Http.enc(query) + "&type=video";
        String body = Http.get(url);
        String[] objs = Json.objects(body);
        ArrayList out = new ArrayList();
        for (int i = 0; i < objs.length; i++) {
            String o = objs[i];
            String id = Json.string(o, "videoId");
            String title = Json.string(o, "title");
            String uploader = Json.string(o, "author");
            int duration = Json.integer(o, "lengthSeconds");
            if (id.length() > 0 && title.length() > 0)
                out.add(new VideoResult(0, title, uploader, g.type, id, g, duration));
        }
        VideoResult[] a = new VideoResult[out.size()];
        out.toArray(a);
        return a;
    }
}
