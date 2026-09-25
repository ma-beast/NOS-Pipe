package notpipe.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class PlaybackManager {
    private final Gateway[] gateways;
    private final File configFile;
    private StatusListener statusListener;
    private int preferredVideoHeight;
    private String playerMode;
    private String playerCommand;
    private int waitingMessageGeneration;

    private static final String[] WAITING_MESSAGES = {
        "CACHE LOADED",
        "STREAM ENABLED",
        "WORLD ENCOUNTERED",
        "ASTEROID MISSED",
        "ARMAGEDDON DISABLED",
        "CAT CONSULTED",
        "MEANING LOCATED",
        "REALITY SYNCHRONIZED",
        "PIXELS NEGOTIATED",
        "INTERNET PERSUADED",
        "MPLAYER GENTLY AWAKENED",
        "VIDEO ALMOST CONVINCED",
        "ROBOT ARTWORK",
        "MUSIC COMPOSED",
        "SONATA WRITING",
        "ARE YOU",
        "GET YOUR SAMPLE"
    };

    public PlaybackManager(Gateway[] gateways) {
        this(gateways, 360, "auto", "", defaultConfigFile());
    }

    public PlaybackManager(Gateway[] gateways, int preferredVideoHeight) {
        this(gateways, preferredVideoHeight, "auto", "", defaultConfigFile());
    }

    public PlaybackManager(Gateway[] gateways, int preferredVideoHeight, File configFile) {
        this(gateways, preferredVideoHeight, "auto", "", configFile);
    }

    public PlaybackManager(Gateway[] gateways, int preferredVideoHeight,
                           String playerMode, String playerCommand, File configFile) {
        this.gateways = gateways;
        this.preferredVideoHeight = preferredVideoHeight > 0 ? preferredVideoHeight : 360;
        this.playerMode = normalizePlayerMode(playerMode);
        this.playerCommand = playerCommand == null ? "" : playerCommand.trim();
        this.configFile = configFile;
    }

    private static File defaultConfigFile() {
        try { return AppPaths.configFile(); }
        catch (Exception e) { throw new IllegalStateException("NOS-Pipe folder not found: " + e); }
    }

    public void setPreferredVideoHeight(int height) {
        if (height > 0) {
            preferredVideoHeight = height;
        }
    }

    public void setStatusListener(StatusListener statusListener) {
        this.statusListener = statusListener;
    }

    private void status(String text) {
        if (statusListener != null) statusListener.status(text);
    }

    public void play(VideoResult result) throws Exception {
        cancelWaitingMessages();
        reloadPlayerSettings();
        if (playAcrossGateways(result)) {
            LastVideoStore.save(configFile, result.videoId);
            return;
        }
        throw new IOException("No playable stream found for " + result.videoId);
    }

    private boolean playAcrossGateways(final VideoResult result) {
        if (preferredVideoHeight != 360 && supportsDualStreams()) {
            Log.info("Manual quality " + preferredVideoHeight
                    + "p: trying separate video + audio streams");
            status("DUAL STREAM: " + preferredVideoHeight + "p");
            if (playAcrossGateways(result, true)) return true;
            Log.info("No usable dual stream; falling back to muxed playback");
            status("DUAL FAILED - MUXED FALLBACK");
        } else if (preferredVideoHeight == 360) {
            Log.info("Default 360p mode: muxed streams only");
        } else {
            Log.info("Player mode " + effectivePlayerMode()
                    + " accepts one URL; using muxed playback");
            status("MUXED PLAYER MODE");
        }
        return playAcrossGateways(result, false);
    }

    private boolean playAcrossGateways(final VideoResult result, final boolean dualStream) {
        final long resolutionStarted = System.currentTimeMillis();
        Set<String> tried = new HashSet<String>();
        java.util.ArrayList list = new java.util.ArrayList();

        /* The search-result gateway has just answered successfully, so it is
           the fastest and most likely playback source. Use it immediately
           when it supplies a muxed stream. */
        if (result.gateway != null) {
            tried.add(result.gateway.api);
            long started = System.currentTimeMillis();
            status("VIDEO: CHECKING " + result.gateway.name);
            Log.info("Preferred gateway probe started: " + result.gateway.name);
            StreamCandidate preferred = probeGateway(result.gateway, result.videoId, null, dualStream);
            Log.info("Preferred gateway probe finished: " + result.gateway.name
                    + "; " + (System.currentTimeMillis() - started) + " ms; "
                    + (preferred == null ? "no " + streamMode(dualStream) + " stream" : "candidate found"));
            if (preferred != null && mediaUrlPlayable(preferred)) {
                return launchCandidate(preferred, resolutionStarted, "preferred gateway");
            }
        }

        /* Preserve the accepted parallel global resolver as the fallback. */
        for (int i = 0; i < gateways.length; i++) {
            Gateway gateway = gateways[i];
            if (tried.contains(gateway.api)) continue;
            tried.add(gateway.api);
            list.add(gateway);
        }

        final Gateway[] probeGateways = new Gateway[list.size()];
        list.toArray(probeGateways);
        final StreamCandidate[] candidates = new StreamCandidate[probeGateways.length];
        Thread[] threads = new Thread[probeGateways.length];

        Log.info("Preferred gateway unavailable; launching " + probeGateways.length
                + " fallback probes in parallel");
        status("VIDEO FALLBACK: 0/" + probeGateways.length);
        final int[] fallbackProgress = new int[] { 0, 0 };

        for (int i = 0; i < probeGateways.length; i++) {
            final int index = i;
            final Gateway gateway = probeGateways[i];
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    long started = System.currentTimeMillis();
                    candidates[index] = probeGateway(gateway, result.videoId, null, dualStream);
                    Log.info("Fallback gateway probe finished: " + gateway.name
                            + "; " + (System.currentTimeMillis() - started) + " ms; "
                            + (candidates[index] == null
                                    ? "no " + streamMode(dualStream) + " stream" : "candidate found"));
                    synchronized (fallbackProgress) {
                        fallbackProgress[0]++;
                        if (candidates[index] != null) fallbackProgress[1]++;
                        status("VIDEO FALLBACK: " + fallbackProgress[0] + "/"
                                + probeGateways.length + "  FOUND: " + fallbackProgress[1]);
                    }
                }
            }, "NOSPipe-Probe-" + i);
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

        /* Try fallback candidates by score. A gateway API can return a signed
           URL that looks valid but is rejected by the media host (for example
           HTTP 403 because the signature is tied to another IP address). */
        while (true) {
            StreamCandidate best = null;
            int bestIndex = -1;
            for (int i = 0; i < candidates.length; i++) {
                StreamCandidate candidate = candidates[i];
                if (candidate != null && (best == null || candidate.score > best.score)) {
                    best = candidate;
                    bestIndex = i;
                }
            }

            if (best == null) {
                Log.info("Stream resolution failed after "
                        + (System.currentTimeMillis() - resolutionStarted) + " ms");
                return false;
            }

            candidates[bestIndex] = null;
            if (mediaUrlPlayable(best)) {
                return launchCandidate(best, resolutionStarted, "parallel fallback");
            }
        }
    }

    private boolean mediaUrlPlayable(StreamCandidate candidate) {
        long started = System.currentTimeMillis();
        status("VIDEO LINK: CHECKING " + candidate.gateway.name);
        try {
            int code = Http.probeMedia(candidate.choice.url);
            boolean accepted = code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_PARTIAL;
            Log.info("Media preflight via " + candidate.gateway.name + ": HTTP " + code
                    + "; " + (System.currentTimeMillis() - started) + " ms; "
                    + (accepted ? "accepted" : "rejected"));
            status(accepted ? "VIDEO LINK: ACCEPTED"
                    : "VIDEO LINK: HTTP " + code + " - FALLBACK");
            if (!accepted) return false;

            if (!candidate.choice.muxed) {
                status("AUDIO LINK: CHECKING " + candidate.gateway.name);
                int audioCode = Http.probeMedia(candidate.choice.audioUrl);
                boolean audioAccepted = audioCode == HttpURLConnection.HTTP_OK
                        || audioCode == HttpURLConnection.HTTP_PARTIAL;
                Log.info("Audio preflight via " + candidate.gateway.name + ": HTTP " + audioCode
                        + "; " + (System.currentTimeMillis() - started) + " ms total; "
                        + (audioAccepted ? "accepted" : "rejected"));
                status(audioAccepted ? "VIDEO + AUDIO ACCEPTED"
                        : "AUDIO LINK: HTTP " + audioCode + " - FALLBACK");
                return audioAccepted;
            }
            return true;
        } catch (Exception e) {
            Log.info("Media preflight via " + candidate.gateway.name + " failed after "
                    + (System.currentTimeMillis() - started) + " ms: " + e);
            status("VIDEO LINK FAILED - FALLBACK");
            return false;
        }
    }

    private boolean launchCandidate(StreamCandidate best, long resolutionStarted, String path) {
        if (best == null || best.choice == null
                || best.choice.url == null || best.choice.url.length() == 0) return false;

        StreamChoice choice = best.choice;
        System.out.println("BEST STREAM via " + best.gateway.name
                + " target=" + preferredVideoHeight + "p selected=" + choice.height + "p "
                + (choice.muxed ? "muxed" : "video+audio"));
        System.out.println(choice.url);
        if (!choice.muxed) System.out.println(choice.audioUrl);
        Log.info("Stream resolved via " + path + " in "
                + (System.currentTimeMillis() - resolutionStarted) + " ms");

        try {
            status("STARTING VIDEO...");
            launchPlayer(choice.url, choice.audioUrl);
            status("VIDEO LAUNCHED");
            startWaitingMessages();
            return true;
        } catch (Exception e) {
            System.out.println("Selected gateway playback failed: " + best.gateway + " : " + e);
            return false;
        }
    }

    private synchronized void cancelWaitingMessages() {
        waitingMessageGeneration++;
    }

    private synchronized int nextWaitingMessageGeneration() {
        return ++waitingMessageGeneration;
    }

    private synchronized boolean isCurrentWaitingGeneration(int generation) {
        return waitingMessageGeneration == generation;
    }

    private void startWaitingMessages() {
        final int generation = nextWaitingMessageGeneration();
        final String[] messages = new String[WAITING_MESSAGES.length];
        System.arraycopy(WAITING_MESSAGES, 0, messages, 0, WAITING_MESSAGES.length);

        Random random = new Random(System.currentTimeMillis());
        for (int i = messages.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            String swap = messages[i];
            messages[i] = messages[j];
            messages[j] = swap;
        }

        Thread waitingMessages = new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i < 4; i++) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (!isCurrentWaitingGeneration(generation)) return;
                    status(messages[i]);
                }
            }
        }, "NOS-Pipe-waiting-messages");
        waitingMessages.setDaemon(true);
        waitingMessages.start();
    }

    private StreamCandidate probeGateway(Gateway gateway, String videoId,
                                         StreamCandidate current, boolean dualStream) {
        try {
            StreamChoice choice;
            if ("PIPED".equals(gateway.type)) {
                choice = resolvePiped(gateway, videoId, dualStream);
            } else if ("YTAPILEGACY".equals(gateway.type)) {
                choice = resolveYtApiLegacy(gateway, videoId);
            } else {
                choice = resolveInvidious(gateway, videoId, dualStream);
            }

            if (choice == null || choice.url == null || choice.url.length() == 0) {
                return current;
            }

            if (dualStream && (choice.muxed || choice.audioUrl == null
                    || choice.audioUrl.length() == 0)) {
                System.out.println("SKIP via " + gateway.name + " (dual stream incomplete)");
                return current;
            }
            if (!dualStream && !choice.muxed) {
                System.out.println("SKIP via " + gateway.name + " (muxed fallback required)");
                return current;
            }

            System.out.println("CANDIDATE via " + gateway.name
                    + " target=" + preferredVideoHeight + "p selected=" + choice.height + "p "
                    + (choice.muxed ? "muxed" : "video+audio"));

            StreamCandidate candidate = new StreamCandidate(gateway, choice, candidateScore(choice));
            if (current == null || candidate.score > current.score) {
                return candidate;
            }
        } catch (Exception e) {
            System.out.println("Gateway failed: " + gateway + " : " + e);
        }
        return current;
    }

    private int candidateScore(StreamChoice choice) {
        int distance = Math.abs(choice.height - preferredVideoHeight);
        int score = 100000 - distance * 100;
        if (choice.height == preferredVideoHeight) score += 5000;
        if (choice.height <= preferredVideoHeight) score += 25;
        if (choice.audioUrl != null && choice.audioUrl.length() > 0) score += 300;
        if (choice.muxed) score += 200;
        return score;
    }

    private StreamChoice resolvePiped(Gateway gateway, String videoId,
                                      boolean dualStream) throws Exception {
        String endpoint = gateway.api + "/streams/" + Http.enc(videoId);
        System.out.println("HTTP " + endpoint);
        String json = Http.get(endpoint);
        String[] objects = Json.objectsContainingKey(json, "url");

        StreamChoice bestMuxed = null;
        int bestMuxedScore = Integer.MIN_VALUE;
        StreamChoice bestVideo = null;
        int bestVideoScore = Integer.MIN_VALUE;
        String bestAudio = null;
        int bestAudioScore = Integer.MIN_VALUE;

        for (int i = 0; i < objects.length; i++) {
            String object = objects[i];
            String url = Json.string(object, "url");
            if (!isLikelyMediaUrl(url)) {
                continue;
            }

            String mime = safe(Json.string(object, "mimeType"));
            String format = safe(Json.string(object, "format"));
            String quality = safe(Json.string(object, "quality"));
            String resolution = safe(Json.string(object, "resolution"));
            String codec = safe(Json.string(object, "codec"));
            int height = Json.integer(object, "height");
            if (height <= 0) {
                height = streamHeight(quality, resolution, format);
            }

            boolean videoOnly = Json.bool(object, "videoOnly", false);
            boolean audioOnly = Json.bool(object, "audioOnly", false);
            boolean hasVideo = containsVideo(mime, format, codec);
            boolean hasAudio = containsAudio(mime, format, codec);

            if (audioOnly || (hasAudio && !hasVideo)) {
                if (isDubbedAudio(object, url)) {
                    System.out.println("PIPED dubbed audio ignored");
                    continue;
                }
                int bitrate = Json.integer(object, "bitrate");
                int audioScore = audioScore(object, url, mime, format, codec, bitrate);
                if (audioScore > bestAudioScore) {
                    bestAudioScore = audioScore;
                    bestAudio = url;
                }
                continue;
            }

            if (!hasVideo || height <= 0) {
                continue;
            }

            boolean muxed = !videoOnly && !audioOnly;
            int score = streamScore(height, muxed, mime, format);
            if (muxed) {
                if (score > bestMuxedScore) {
                    bestMuxedScore = score;
                    bestMuxed = new StreamChoice(url, height, true, null);
                }
            } else if (score > bestVideoScore) {
                bestVideoScore = score;
                bestVideo = new StreamChoice(url, height, false, null);
            }
        }

        if (bestMuxed != null) {
            System.out.println("PIPED muxed candidate: " + bestMuxed.height + "p");
        }
        if (bestVideo != null) {
            System.out.println("PIPED video candidate: " + bestVideo.height + "p");
        }
        if (bestAudio != null) {
            System.out.println("PIPED audio candidate found");
        }

        if (dualStream) {
            return bestVideo != null && bestAudio != null
                    ? new StreamChoice(bestVideo.url, bestVideo.height, false, bestAudio) : null;
        }
        return bestMuxed;
    }

    private StreamChoice resolveInvidious(Gateway gateway, String videoId,
                                          boolean dualStream) throws Exception {
        String endpoint = gateway.api + "/api/v1/videos/" + Http.enc(videoId);
        System.out.println("HTTP " + endpoint);
        String json = Http.get(endpoint);
        String[] objects = Json.objectsContainingKey(json, "url");

        StreamChoice bestMuxed = null;
        int bestMuxedScore = Integer.MIN_VALUE;
        StreamChoice bestVideo = null;
        int bestVideoScore = Integer.MIN_VALUE;
        String bestAudio = null;
        int bestAudioScore = Integer.MIN_VALUE;

        for (int i = 0; i < objects.length; i++) {
            String object = objects[i];
            String url = Json.string(object, "url");
            if (!isLikelyMediaUrl(url)) {
                continue;
            }

            String mime = safe(Json.string(object, "mimeType"));
            String type = safe(Json.string(object, "type"));
            if (mime.length() == 0) mime = type;
            String itag = safe(Json.string(object, "itag"));
            String quality = safe(Json.string(object, "quality"));
            String resolution = safe(Json.string(object, "resolution"));
            String label = safe(Json.string(object, "qualityLabel"));
            int height = Json.integer(object, "height");
            if (height <= 0) height = streamHeight(quality, resolution, label);

            boolean muxed = isMuxedInvidious(object, itag, type);
            boolean audio = isAudioObject(object, mime, type);

            if (audio && !muxed) {
                if (isDubbedAudio(object, url)) {
                    System.out.println("INVIDIOUS dubbed audio ignored");
                    continue;
                }
                int bitrate = Json.integer(object, "bitrate");
                int audioScore = audioScore(object, url, mime, type, "", bitrate);
                if (audioScore > bestAudioScore) {
                    bestAudioScore = audioScore;
                    bestAudio = url;
                }
                continue;
            }

            if (!containsVideo(mime, type, "") || height <= 0) {
                continue;
            }

            int score = streamScore(height, muxed, mime, type);
            if (muxed) {
                if (score > bestMuxedScore) {
                    bestMuxedScore = score;
                    bestMuxed = new StreamChoice(url, height, true, null);
                }
            } else if (score > bestVideoScore) {
                bestVideoScore = score;
                bestVideo = new StreamChoice(url, height, false, null);
            }
        }

        if (bestMuxed != null) {
            System.out.println("INVIDIOUS muxed candidate: " + bestMuxed.height + "p");
        }
        if (bestVideo != null) {
            System.out.println("INVIDIOUS video candidate: " + bestVideo.height + "p");
        }
        if (bestAudio != null) {
            System.out.println("INVIDIOUS audio candidate found");
        }

        if (dualStream) {
            return bestVideo != null && bestAudio != null
                    ? new StreamChoice(bestVideo.url, bestVideo.height, false, bestAudio) : null;
        }
        return bestMuxed;
    }

    private StreamChoice resolveYtApiLegacy(Gateway gateway, String videoId) throws Exception {
        return null;
    }

    private boolean isMuxedInvidious(String object, String itag, String type) {
        if ("17".equals(itag) || "18".equals(itag) || "22".equals(itag)
                || "37".equals(itag) || "38".equals(itag)) {
            return true;
        }
        String t = safe(type).toLowerCase();
        if (t.indexOf("video/") >= 0
                && (t.indexOf("mp4a") >= 0 || t.indexOf("opus") >= 0 || t.indexOf("vorbis") >= 0)) {
            return true;
        }
        // audioQuality/audioSampleRate/audioChannels identify AUDIO-ONLY
        // adaptive entries, not muxed video. Do not use them as muxed markers.
        return false;
    }

    private boolean isAudioObject(String object, String mime, String type) {
        String all = (safe(mime) + " " + safe(type)).toLowerCase();
        if (all.indexOf("audio/") >= 0 || all.indexOf("mp4a") >= 0
                || all.indexOf("opus") >= 0 || all.indexOf("vorbis") >= 0) {
            return true;
        }
        return Json.string(object, "audioQuality").length() > 0
                && all.indexOf("video/") < 0;
    }

    private boolean containsVideo(String mime, String format, String codec) {
        String all = (safe(mime) + " " + safe(format) + " " + safe(codec)).toLowerCase();
        return all.indexOf("video/") >= 0 || all.indexOf("video") >= 0
                || (all.indexOf("mp4") >= 0 && all.indexOf("audio") < 0);
    }

    private boolean containsAudio(String mime, String format, String codec) {
        String all = (safe(mime) + " " + safe(format) + " " + safe(codec)).toLowerCase();
        return all.indexOf("audio/") >= 0 || all.indexOf("audio") >= 0
                || all.indexOf("mp4a") >= 0 || all.indexOf("opus") >= 0 || all.indexOf("vorbis") >= 0;
    }

    private int streamScore(int height, boolean muxed, String mime, String format) {
        int distance = Math.abs(height - preferredVideoHeight);
        int score = 100000 - distance * 100;
        if (height == preferredVideoHeight) score += 5000;
        if (height <= preferredVideoHeight) score += 25;
        if (muxed) score += 500;
        if (safe(mime).toLowerCase().indexOf("video/mp4") >= 0) score += 100;
        String f = safe(format).toLowerCase();
        if (f.indexOf("mpeg_4") >= 0 || f.indexOf("mp4") >= 0) score += 20;
        return score;
    }

    private int audioScore(String object, String url, String mime, String format,
                           String codec, int bitrate) {
        String all = (safe(mime) + " " + safe(format) + " " + safe(codec)).toLowerCase();
        int score = Math.max(0, bitrate);
        String trackType = safe(Json.string(object, "audioTrackType")).toLowerCase();
        String lowerUrl = safe(url).toLowerCase();
        if (trackType.indexOf("original") >= 0
                || lowerUrl.indexOf("acont%3doriginal") >= 0
                || lowerUrl.indexOf("acont=original") >= 0) score += 3000000;
        if (Json.bool(object, "audioIsDefault", false)) score += 2000000;
        if (all.indexOf("mp4a") >= 0 || all.indexOf("aac") >= 0) score += 1000000;
        else if (all.indexOf("audio/mp4") >= 0 || all.indexOf("m4a") >= 0) score += 900000;
        else if (all.indexOf("vorbis") >= 0) score += 200000;
        else if (all.indexOf("opus") >= 0) score += 100000;
        return score;
    }

    private boolean isDubbedAudio(String object, String url) {
        String trackType = safe(Json.string(object, "audioTrackType")).toLowerCase();
        String lowerUrl = safe(url).toLowerCase();
        return trackType.indexOf("dub") >= 0
                || Json.bool(object, "audioTrackIsAutoDubbed", false)
                || lowerUrl.indexOf("acont%3ddubbed") >= 0
                || lowerUrl.indexOf("acont=dubbed") >= 0;
    }

    private boolean isLikelyMediaUrl(String url) {
        if (url == null || url.length() == 0) return false;
        String lower = url.toLowerCase();
        if (lower.indexOf("yt3.ggpht.com/") >= 0
                || lower.indexOf("i.ytimg.com/") >= 0
                || lower.indexOf("img.youtube.com/") >= 0) return false;
        int q = lower.indexOf('?');
        String path = q >= 0 ? lower.substring(0, q) : lower;
        return !(path.endsWith(".jpg") || path.endsWith(".jpeg")
                || path.endsWith(".png") || path.endsWith(".webp") || path.endsWith(".gif"));
    }

    private int streamHeight(String quality, String resolution, String label) {
        int h = parseHeight(resolution);
        if (h > 0) return h;
        h = parseHeight(quality);
        if (h > 0) return h;
        return parseHeight(label);
    }

    private int parseHeight(String text) {
        if (text == null) return -1;
        int x = text.indexOf('x');
        if (x > 0 && x + 1 < text.length()) {
            try {
                String s = text.substring(x + 1).replaceAll("[^0-9].*", "");
                if (s.length() > 0) return Integer.parseInt(s);
            } catch (Exception e) { }
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                int j = i;
                while (j < text.length()) {
                    char d = text.charAt(j);
                    if (d < '0' || d > '9') break;
                    j++;
                }
                if (j < text.length() && text.charAt(j) == 'p') {
                    try { return Integer.parseInt(text.substring(i, j)); }
                    catch (Exception e) { return -1; }
                }
                i = j;
            }
        }
        return -1;
    }

    private void launchPlayer(String videoUrl, String audioUrl) throws Exception {
        File baseDir = Log.applicationDirectory();
        String mode = effectivePlayerMode();
        ProcessBuilder builder;
        LegacyHttpBridge bridge = null;
        boolean bundledMplayer = "mplayer".equals(mode);
        if (bundledMplayer) {
            File mplayer = bundledMplayerFile(baseDir);
            if (!mplayer.isFile()) {
                throw new IOException(mplayer.getName() + " not found beside NOS-Pipe.jar: "
                        + mplayer.getAbsolutePath());
            }
            if (isMac() && (audioUrl == null || audioUrl.length() == 0)) {
                bridge = LegacyHttpBridge.start(videoUrl);
                videoUrl = bridge.localUrl();
                Log.info("Mac legacy MPlayer uses local HTTP 200 bridge");
                builder = new ProcessBuilder(mplayer.getAbsolutePath(), videoUrl);
            } else if (audioUrl != null && audioUrl.length() > 0) {
                builder = new ProcessBuilder(mplayer.getAbsolutePath(), "-audiofile", audioUrl, videoUrl);
            } else {
                builder = new ProcessBuilder(mplayer.getAbsolutePath(), videoUrl);
            }
        } else if ("system".equals(mode)) {
            if (audioUrl != null && audioUrl.length() > 0) {
                throw new IOException("System player mode accepts muxed streams only");
            }
            builder = systemPlayer(videoUrl);
        } else {
            builder = customPlayer(videoUrl, audioUrl);
        }
        builder.directory(baseDir);
        builder.redirectErrorStream(true);

        Log.info("Starting video player; player=" + mode + "; stream="
                + (audioUrl == null || audioUrl.length() == 0 ? "muxed" : "video+audio")
                + "; video URL length=" + videoUrl.length()
                + (audioUrl == null ? "" : "; audio URL length=" + audioUrl.length()));
        final LegacyHttpBridge playerBridge = bridge;
        final Process process;
        try {
            process = builder.start();
        } catch (Exception e) {
            if (playerBridge != null) playerBridge.stop();
            throw e;
        }
        Log.info("Video player process created");

        /* MPlayer is not in slave mode. Closing its command input immediately
           prevents old Windows/JVM combinations from keeping it waiting on the
           NOS-Pipe console. */
        process.getOutputStream().close();
        Log.info("Video player command input closed");

        /* Drain merged stdout/stderr so the child can never block on a full
           pipe. The thread also retains the Process object until it exits. */
        Thread outputReader = new Thread(new Runnable() {
            public void run() {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[Video player] " + line);
                    }
                    reader.close();
                    Log.info("Video player exited; code=" + process.waitFor());
                } catch (Exception e) {
                    Log.info("Video player output reader stopped: " + e);
                } finally {
                    if (playerBridge != null) {
                        playerBridge.stop();
                        Log.info("Legacy HTTP bridge closed with video player");
                    }
                }
            }
        }, "NOS-Pipe-player-output");
        outputReader.setDaemon(true);
        outputReader.start();
        if (bundledMplayer && audioUrl != null && audioUrl.length() > 0) {
            try {
                Thread.sleep(500);
                int earlyCode = process.exitValue();
                throw new IOException("Dual-stream MPlayer exited during startup; code=" + earlyCode);
            } catch (IllegalThreadStateException stillRunning) {
                /* The player survived its startup window. Continue asynchronously. */
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        Log.info("Video launched");
    }

    private void reloadPlayerSettings() {
        try {
            UiConfig ui = UiConfig.load(configFile);
            preferredVideoHeight = ui.videoHeight;
            playerMode = normalizePlayerMode(ui.playerMode);
            playerCommand = ui.playerCommand;
            Log.info("Playback settings: config=" + configFile.getAbsolutePath()
                    + "; height=" + preferredVideoHeight
                    + "; mode=" + playerMode
                    + "; effective=" + effectivePlayerMode());
        } catch (Exception e) {
            Log.info("Player settings reload failed; keeping current values: " + e);
        }
    }

    private boolean supportsDualStreams() {
        String mode = effectivePlayerMode();
        if ("mplayer".equals(mode) && isMac()) return false;
        return "mplayer".equals(mode)
                || ("custom".equals(mode) && playerCommand.indexOf("{audio}") >= 0);
    }

    private String effectivePlayerMode() {
        String mode = normalizePlayerMode(playerMode);
        if (!"auto".equals(mode)) return mode;
        try {
            if (bundledMplayerFile(Log.applicationDirectory()).isFile()) return "mplayer";
        } catch (Exception e) { }
        return "system";
    }

    private static String normalizePlayerMode(String mode) {
        String value = mode == null ? "auto" : mode.trim().toLowerCase();
        if ("mplayer".equals(value) || "system".equals(value)
                || "custom".equals(value) || "auto".equals(value)) return value;
        return "auto";
    }

    private File bundledMplayerFile(File baseDir) {
        if (isWindows()) return new File(baseDir, "mplayer.exe");
        if (isMac()) {
            String arch = System.getProperty("os.arch", "").toLowerCase();
            if (arch.indexOf("ppc") >= 0 || arch.indexOf("powerpc") >= 0) {
                return new File(baseDir, "mplayer~.ppc");
            }
            return new File(baseDir, "mplayer~.x86");
        }
        return new File(baseDir, "mplayer");
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().indexOf("mac") >= 0;
    }

    private ProcessBuilder systemPlayer(String videoUrl) throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.indexOf("windows") >= 0) {
            return new ProcessBuilder("rundll32.exe", "url.dll,FileProtocolHandler", videoUrl);
        }
        if (os.indexOf("mac") >= 0) {
            return new ProcessBuilder("open", videoUrl);
        }
        return new ProcessBuilder("xdg-open", videoUrl);
    }

    private ProcessBuilder customPlayer(String videoUrl, String audioUrl) throws IOException {
        if (playerCommand == null || playerCommand.trim().length() == 0) {
            throw new IOException("Custom player command is empty");
        }
        List tokens = splitCommand(playerCommand);
        ArrayList args = new ArrayList();
        boolean hasVideo = false;
        for (int i = 0; i < tokens.size(); i++) {
            String token = (String)tokens.get(i);
            if (token.indexOf("{audio}") >= 0 && (audioUrl == null || audioUrl.length() == 0)) {
                if ("{audio}".equals(token) && !args.isEmpty()
                        && "-audiofile".equals(args.get(args.size() - 1))) {
                    args.remove(args.size() - 1);
                }
                continue;
            }
            if (token.indexOf("{video}") >= 0) hasVideo = true;
            token = replace(token, "{video}", videoUrl);
            token = replace(token, "{audio}", audioUrl == null ? "" : audioUrl);
            args.add(token);
        }
        if (!hasVideo) args.add(videoUrl);
        if (args.isEmpty()) throw new IOException("Custom player command is empty");
        String[] command = new String[args.size()];
        args.toArray(command);
        return new ProcessBuilder(command);
    }

    private List splitCommand(String command) throws IOException {
        ArrayList out = new ArrayList();
        StringBuffer token = new StringBuffer();
        char quote = 0;
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
                else token.append(c);
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (Character.isWhitespace(c)) {
                if (token.length() > 0) {
                    out.add(token.toString());
                    token.setLength(0);
                }
            } else {
                token.append(c);
            }
        }
        if (quote != 0) throw new IOException("Unclosed quote in custom player command");
        if (token.length() > 0) out.add(token.toString());
        return out;
    }

    private String replace(String text, String from, String to) {
        int at = text.indexOf(from);
        while (at >= 0) {
            text = text.substring(0, at) + to + text.substring(at + from.length());
            at = text.indexOf(from, at + to.length());
        }
        return text;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().indexOf("windows") >= 0;
    }

    private String safe(String s) { return s == null ? "" : s; }

    private String streamMode(boolean dualStream) {
        return dualStream ? "video+audio" : "muxed";
    }

    private static final class StreamCandidate {
        final Gateway gateway;
        final StreamChoice choice;
        final int score;

        StreamCandidate(Gateway gateway, StreamChoice choice, int score) {
            this.gateway = gateway;
            this.choice = choice;
            this.score = score;
        }
    }

    private static final class StreamChoice {
        final String url;
        final int height;
        final boolean muxed;
        final String audioUrl;

        StreamChoice(String url, int height, boolean muxed, String audioUrl) {
            this.url = url;
            this.height = height;
            this.muxed = muxed;
            this.audioUrl = audioUrl;
        }
    }
}
