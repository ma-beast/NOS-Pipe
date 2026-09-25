package notpipe.gui;

import java.awt.Frame;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

public final class NOSPipeGui {
    public static void main(String[] args) throws Exception {
        Log.init();
        final boolean explicitSearch = args.length > 0;
        final String query = explicitSearch ? joinArgs(args) : "";

        final File config = AppPaths.configFile();
        if (!config.exists()) throw new IllegalStateException("Missing gateways.properties");
        final Gateway[] gateways = GatewayConfig.load(config);
        UiConfig ui = GatewayConfig.loadUiConfig(config);
        if (gateways.length == 0) throw new IllegalStateException("No enabled gateways in gateways.properties");

        final String lastVideoId = explicitSearch ? "" : LastVideoStore.load(config);
        final String startupLabel = explicitSearch ? query
                : (lastVideoId.length() == 0 ? "POPULAR" : "RELATED");
        Log.info(explicitSearch ? "Startup search: " + query
                : "Startup feed: " + startupLabel
                        + (lastVideoId.length() == 0 ? "" : "; video=" + lastVideoId));

        System.out.println("Gateway order:");
        for (int i = 0; i < gateways.length; i++) System.out.println("  " + (i + 1) + ". " + gateways[i]);

        final Frame frame = new Frame("NOS Pipe");
        final PlaybackManager manager = new PlaybackManager(gateways, ui.videoHeight,
                ui.playerMode, ui.playerCommand, config);
        final JTubeCanvas canvas = new JTubeCanvas(new java.util.ArrayList(), manager, startupLabel, ui.fontSize);
        manager.setStatusListener(canvas);
        canvas.applySettings(ui.fontSize, ui.videoHeight);
        canvas.setMessage("LOADING: " + startupLabel);
        restoreWindow(frame, ui);
        frame.add(canvas);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                WindowStateStore.save(config, frame.getBounds());
                System.exit(0);
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() { WindowStateStore.save(config, frame.getBounds()); }
        }, "NOS-Pipe-save-window"));
        frame.setVisible(true);
        canvas.requestFocus();

        Thread searchThread = new Thread(new Runnable() {
            public void run() {
                try {
                    List results;
                    if (explicitSearch) {
                        results = new SearchCoordinator(gateways, canvas).search(query);
                    } else {
                        StartupFeedCoordinator feed = new StartupFeedCoordinator(gateways, canvas);
                        if (lastVideoId.length() == 0) {
                            results = feed.popular();
                        } else {
                            results = feed.related(lastVideoId);
                            if (results.isEmpty()) {
                                Log.info("RELATED unavailable; falling back to POPULAR");
                                results = feed.popular();
                            }
                        }
                    }
                    System.out.println("Results: " + results.size());
                    canvas.setResults(results);
                    if (results.isEmpty()) {
                        canvas.setMessage("NO RESULTS");
                    } else {
                        canvas.setMessage("READY: " + results.size() + " RESULTS");
                    }
                } catch (Exception e) {
                    System.out.println("Search failed: " + e);
                    canvas.setMessage("SEARCH FAILED: " + e.getMessage());
                }
            }
        }, "NOS-Pipe-startup-feed");
        searchThread.setDaemon(true);
        searchThread.start();
    }

    private static String joinArgs(String[] args) {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) b.append(' ');
            b.append(args[i]);
        }
        return b.toString();
    }

    private static void restoreWindow(Frame frame, UiConfig ui) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(screen.width, Math.max(240, ui.windowWidth));
        int height = Math.min(screen.height, Math.max(160, ui.windowHeight));
        int x = ui.windowX;
        int y = ui.windowY;
        if (x < 0 || y < 0 || x >= screen.width - 40 || y >= screen.height - 40) {
            x = Math.max(0, (screen.width - width) / 2);
            y = Math.max(0, (screen.height - height) / 2);
        }
        if (x + width > screen.width) x = Math.max(0, screen.width - width);
        if (y + height > screen.height) y = Math.max(0, screen.height - height);
        frame.setBounds(x, y, width, height);
    }
}
