package notpipe.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Loads the startup Popular or Related list without changing manual search. */
public final class StartupFeedCoordinator {
    private static final String[] HYPE_PLAYLISTS = new String[] {
        "OLXuPDPDe3URMWBdmS4_jxdqVF08DAlAXWQ",
        "OLd7LoUR1ndxIsyTJ_5pK4tvrWBcvkewBLg",
        "OLNUaVf-BhE20xpEEdOmnxF6oyKQwpN1tBQ",
        "OLUZ2nAWnPVwwoXomaTvbFi1Cje8td4Z0zg",
        "OLewrgOzMLeOADYzQ1ewNeqtPlJP20RE_Zg",
        "OLbXum44nJ19cZmR7GxDm5rF_Nj_coRoO5g",
        "OLPPB3977IfFUtPW8213fFOsf2lmAWfahDg",
        "OLZyZj8vWFMkIOGlFzyKuFlLA2PY42VhRuA"
    };

    private final Gateway[] gateways;
    private final StatusListener statusListener;

    public StartupFeedCoordinator(Gateway[] gateways) {
        this(gateways, null);
    }

    public StartupFeedCoordinator(Gateway[] gateways, StatusListener statusListener) {
        this.gateways = gateways;
        this.statusListener = statusListener;
    }

    public List popular() {
        long day = System.currentTimeMillis() / 86400000L;
        int index = (int)(day % HYPE_PLAYLISTS.length);
        if (index < 0) index = -index;
        return load(false, HYPE_PLAYLISTS[index], "POPULAR");
    }

    public List related(String videoId) {
        return load(true, videoId, "RELATED");
    }

    private List load(final boolean related, final String value, final String label) {
        final VideoResult[][] results = new VideoResult[gateways.length][];
        final int[] progress = new int[] { 0, 0 };
        Thread[] threads = new Thread[gateways.length];
        Log.info("Loading startup " + label + " from " + gateways.length + " gateways");
        status(label + " GATEWAYS: 0/" + gateways.length);

        for (int i = 0; i < gateways.length; i++) {
            final int index = i;
            final Gateway gateway = gateways[i];
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        results[index] = related
                                ? relatedFrom(gateway, value)
                                : popularFrom(gateway, value);
                        Log.info("Startup feed via " + gateway.name + ": "
                                + results[index].length + " items");
                    } catch (Exception e) {
                        Log.info("Startup feed failed via " + gateway.name + ": " + e);
                    } finally {
                        synchronized (progress) {
                            progress[0]++;
                            if (results[index] != null) progress[1] += results[index].length;
                            status(label + " GATEWAYS: " + progress[0] + "/" + gateways.length
                                    + "  FOUND: " + progress[1]);
                        }
                    }
                }
            }, "NOS-Pipe-home-" + i);
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

        ArrayList merged = new ArrayList();
        Set ids = new HashSet();
        for (int i = 0; i < gateways.length; i++) {
            VideoResult[] found = results[i];
            if (found == null) continue;
            for (int j = 0; j < found.length; j++) {
                VideoResult video = found[j];
                if (video.videoId.length() == 0 || video.videoId.equals(value)
                        || ids.contains(video.videoId)) continue;
                ids.add(video.videoId);
                merged.add(new VideoResult(merged.size() + 1, video.title, video.author,
                        video.backend, video.videoId, gateways[i], video.durationSeconds));
            }
        }
        Log.info("Startup " + label + " ready: " + merged.size() + " unique items");
        return merged;
    }

    private void status(String text) {
        if (statusListener != null) statusListener.status(text);
    }

    private VideoResult[] popularFrom(Gateway gateway, String playlistId) throws Exception {
        String endpoint;
        String arrayKey;
        if ("PIPED".equals(gateway.type)) {
            endpoint = gateway.api + "/playlists/" + Http.enc(playlistId);
            arrayKey = "relatedStreams";
        } else {
            endpoint = gateway.api + "/api/v1/playlists/" + Http.enc(playlistId);
            arrayKey = "videos";
        }
        return parseFeed(gateway, Http.get(endpoint), arrayKey);
    }

    private VideoResult[] relatedFrom(Gateway gateway, String videoId) throws Exception {
        String endpoint;
        String arrayKey;
        if ("PIPED".equals(gateway.type)) {
            endpoint = gateway.api + "/streams/" + Http.enc(videoId);
            arrayKey = "relatedStreams";
        } else {
            endpoint = gateway.api + "/api/v1/videos/" + Http.enc(videoId);
            arrayKey = "recommendedVideos";
        }
        return parseFeed(gateway, Http.get(endpoint), arrayKey);
    }

    private VideoResult[] parseFeed(Gateway gateway, String body, String arrayKey) {
        String[] objects = Json.arrayObjects(body, arrayKey);
        ArrayList videos = new ArrayList();
        for (int i = 0; i < objects.length; i++) {
            String object = objects[i];
            String id;
            String author;
            if ("PIPED".equals(gateway.type)) {
                id = Json.string(object, "url");
                int marker = id.indexOf("/watch?v=");
                if (marker >= 0) id = id.substring(marker + 9);
                int amp = id.indexOf('&');
                if (amp >= 0) id = id.substring(0, amp);
                author = Json.string(object, "uploaderName");
            } else {
                id = Json.string(object, "videoId");
                author = Json.string(object, "author");
            }
            String title = Json.string(object, "title");
            int duration = "PIPED".equals(gateway.type)
                    ? Json.integer(object, "duration")
                    : Json.integer(object, "lengthSeconds");
            if (id.length() > 0 && title.length() > 0) {
                videos.add(new VideoResult(0, title, author, gateway.type, id, gateway, duration));
            }
        }
        VideoResult[] array = new VideoResult[videos.size()];
        videos.toArray(array);
        return array;
    }
}
