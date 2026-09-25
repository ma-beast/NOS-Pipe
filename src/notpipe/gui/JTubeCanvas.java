package notpipe.gui;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.BasicStroke;
import java.awt.Polygon;
import java.awt.Frame;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Dialog;
import java.awt.Label;
import java.awt.TextField;
import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.EventQueue;
import java.awt.Insets;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;

import javax.imageio.ImageIO;

public final class JTubeCanvas extends Canvas
        implements KeyListener, MouseListener, MouseWheelListener, StatusListener {

    private volatile List<VideoResult> results;
    private int selected;
    private int firstVisible;
    private volatile String message = "READY";
    private final PlaybackManager playback;
    private String query;
    private boolean playbackBusy;
    private int fontSize;
    private int videoHeight;
    /* Keyboard focus for the four bottom/right controls. */
    private int activeControl = CONTROL_PLAY;

    private static final int CONTROL_SIZE = 34;
    private static final int CONTROL_GAP = 8;
    private static final int CONTROL_MARGIN = 6;
    private static final int CONTROL_BAR_H = CONTROL_SIZE + CONTROL_MARGIN * 2;

    private static final int CONTROL_PLAY = 0;
    private static final int CONTROL_SEARCH = 1;
    private static final int CONTROL_SETTINGS = 2;
    private static final int CONTROL_EXIT = 3;

    /* Small cache: deliberately bounded for old/low-memory machines. */
    private final Map<String, Image> thumbs = new HashMap<String, Image>();
    private final LinkedList<String> thumbOrder = new LinkedList<String>();
    private final Map<String, Boolean> thumbLoading = new HashMap<String, Boolean>();

    public JTubeCanvas(List<VideoResult> results) {
        this(results, null, "", 14);
    }

    public JTubeCanvas(List<VideoResult> results, PlaybackManager playback, String query) {
        this(results, playback, query, 14);
    }

    public JTubeCanvas(List<VideoResult> results, PlaybackManager playback, String query, int fontSize) {
        this.results = results;
        this.playback = playback;
        this.query = query;
        this.fontSize = (fontSize >= 8 && fontSize <= 24) ? fontSize : 14;
        this.videoHeight = 360;
        setBackground(Color.black);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseWheelListener(this);
    }

    public void paint(Graphics g) {
        int w = getWidth(), h = getHeight();
        int mode = LayoutMode.detect(w, h);

        g.setColor(Color.black);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.white);
        int headerSize = fontSize + 2;
        g.setFont(new Font("Dialog", Font.BOLD, headerSize));
        g.drawString(headerText(g.getFontMetrics(), mode, w, h), 8, headerSize + 5);

        if (mode == LayoutMode.PORTRAIT) paintPortrait(g, w, h);
        else if (mode == LayoutMode.ULTRAWIDE) paintUltrawide(g, w, h);
        else paintLandscape(g, w, h);

        paintControls(g, w, h, mode);

        g.setColor(Color.lightGray);
        g.setFont(new Font("Dialog", Font.PLAIN, fontSize));
        int statusWidth = mode == LayoutMode.ULTRAWIDE
                ? w - CONTROL_SIZE - CONTROL_MARGIN * 3 - 8 : w - 16;
        g.drawString(fitText(message, g.getFontMetrics(), statusWidth), 8,
                mode == LayoutMode.ULTRAWIDE ? h - 8 : h - CONTROL_BAR_H - 5);
    }

    private int visibleCount(int mode, int w, int h) {
        if (mode == LayoutMode.PORTRAIT)
            return Math.max(1, (h - contentTop() - CONTROL_BAR_H - 4) / portraitRowHeight());
        if (mode == LayoutMode.ULTRAWIDE) {
            int usableW = Math.max(1, w - CONTROL_SIZE - CONTROL_MARGIN * 3);
            int cardW = 170;
            return Math.max(1, usableW / cardW);
        }
        return Math.max(1, (h - contentTop() - CONTROL_BAR_H - 4) / landscapeRowHeight());
    }

    private int textLineHeight() {
        return fontSize + 3;
    }

    private int landscapeRowHeight() {
        return Math.max(56, textLineHeight() * 4 + 6);
    }

    private int portraitRowHeight() {
        return 58 + textLineHeight() * 4;
    }

    private int contentTop() {
        return fontSize + 12;
    }

    private String headerText(FontMetrics metrics, int mode, int w, int h) {
        int start = results.isEmpty() ? 0 : firstVisible + 1;
        String suffix = "  " + start + "-" + visibleEnd(mode, h) + " / " + results.size();
        String label = query == null ? "" : query;
        int maxWidth = Math.max(1, w - 16);
        if (metrics.stringWidth(label + suffix) <= maxWidth) return label + suffix;

        String dots = "...";
        while (label.length() > 0
                && metrics.stringWidth(label + dots + suffix) > maxWidth) {
            label = label.substring(0, label.length() - 1);
        }
        return label + (label.length() == 0 ? "" : dots) + suffix;
    }

    private String fitText(String text, FontMetrics metrics, int maxWidth) {
        if (text == null) return "";
        if (metrics.stringWidth(text) <= maxWidth) return text;
        String dots = "...";
        while (text.length() > 0
                && metrics.stringWidth(text + dots) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + (text.length() == 0 ? "" : dots);
    }

    private int visibleEnd(int mode, int h) {
        int count = visibleCount(mode, getWidth(), h);
        if (results.isEmpty()) return 0;
        return Math.min(results.size(), firstVisible + count);
    }

    private void ensureVisible() {
        int mode = LayoutMode.detect(getWidth(), getHeight());
        int count = visibleCount(mode, getWidth(), getHeight());
        if (selected < firstVisible) firstVisible = selected;
        if (selected >= firstVisible + count) firstVisible = selected - count + 1;
        clampFirstVisible(count);
    }

    private void clampFirstVisible(int count) {
        int max = Math.max(0, results.size() - count);
        if (firstVisible > max) firstVisible = max;
        if (firstVisible < 0) firstVisible = 0;
    }

    private void paintLandscape(Graphics g, int w, int h) {
        int top = contentTop();
        int rowH = landscapeRowHeight();
        int count = visibleCount(LayoutMode.LANDSCAPE, w, h);
        for (int n = 0; n < count; n++) {
            int i = firstVisible + n;
            if (i >= results.size()) break;
            VideoResult v = results.get(i);
            int y = top + n * rowH;
            Graphics card = g.create(4, y, w - 8, rowH - 2);
            drawSelection(card, i, 0, 0, w - 8, rowH - 2);
            drawThumb(card, v, 4, 4, 82, 46);
            card.setColor(Color.white);
            int lineH = textLineHeight();
            int textW = Math.max(1, w - 108);
            drawNumberDuration(card, v, 92, lineH);
            card.setFont(new Font("Dialog", Font.BOLD, fontSize));
            String[] title = wrapTwoLines(v.title, card.getFontMetrics(), textW);
            card.drawString(title[0], 92, lineH * 2);
            if (title[1].length() > 0) card.drawString(title[1], 92, lineH * 3);
            card.setFont(new Font("Dialog", Font.PLAIN, fontSize));
            card.drawString(fitText(v.author, card.getFontMetrics(), textW), 92,
                    title[1].length() > 0 ? lineH * 4 : lineH * 3);
            card.dispose();
        }
    }

    private void paintPortrait(Graphics g, int w, int h) {
        int top = contentTop();
        int rowH = portraitRowHeight();
        int count = visibleCount(LayoutMode.PORTRAIT, w, h);
        for (int n = 0; n < count; n++) {
            int i = firstVisible + n;
            if (i >= results.size()) break;
            VideoResult v = results.get(i);
            int y = top + n * rowH;
            Graphics card = g.create(4, y, w - 8, rowH - 2);
            drawSelection(card, i, 0, 0, w - 8, rowH - 2);
            drawThumb(card, v, 4, 4, Math.max(100, w - 16), 50);
            card.setColor(Color.white);
            int lineH = textLineHeight();
            int textW = Math.max(1, w - 16);
            drawNumberDuration(card, v, 4, 54 + lineH);
            card.setFont(new Font("Dialog", Font.BOLD, fontSize));
            String[] title = wrapTwoLines(v.title, card.getFontMetrics(), textW);
            card.drawString(title[0], 4, 54 + lineH * 2);
            if (title[1].length() > 0) card.drawString(title[1], 4, 54 + lineH * 3);
            card.setFont(new Font("Dialog", Font.PLAIN, fontSize));
            card.drawString(fitText(v.author, card.getFontMetrics(), textW), 4,
                    54 + (title[1].length() > 0 ? lineH * 4 : lineH * 3));
            card.dispose();
        }
    }

    private void paintUltrawide(Graphics g, int w, int h) {
        /* 640x200-class screens are horizontal shelves: video cards go left-to-right.
         * The four hardware controls remain in the right-hand strip. */
        int top = contentTop();
        int right = w - CONTROL_SIZE - CONTROL_MARGIN * 2;
        int usableW = Math.max(1, right - 6);
        int count = visibleCount(LayoutMode.ULTRAWIDE, w, h);
        int cardW = Math.max(130, usableW / count);
        int cardH = Math.max(1, h - top - 24);

        for (int n = 0; n < count; n++) {
            int i = firstVisible + n;
            if (i >= results.size()) break;
            VideoResult v = results.get(i);
            int x = 4 + n * cardW;
            Graphics card = g.create(x, top, cardW - 4, cardH);
            drawSelection(card, i, 0, 0, cardW - 4, cardH);

            int lineH = textLineHeight();
            int wantedThumbW = Math.min(cardW - 16, Math.max(72, (cardW - 16) * 16 / 20));
            int thumbH = Math.min(Math.min(62, Math.max(40, wantedThumbW * 9 / 16)),
                    Math.max(24, cardH - lineH * 4 - 8));
            int thumbW = Math.min(wantedThumbW, Math.max(42, thumbH * 16 / 9));
            int tx = (cardW - thumbW) / 2;
            drawThumb(card, v, tx, 5, thumbW, thumbH);

            card.setColor(Color.white);
            drawNumberDuration(card, v, 6, thumbH + lineH);
            card.setFont(new Font("Dialog", Font.BOLD, fontSize));
            String[] title = wrapTwoLines(v.title, card.getFontMetrics(), cardW - 16);
            card.drawString(title[0], 6, thumbH + lineH * 2);
            if (title[1].length() > 0) card.drawString(title[1], 6, thumbH + lineH * 3);
            card.setFont(new Font("Dialog", Font.PLAIN, fontSize));
            card.drawString(fitText(v.author, card.getFontMetrics(), cardW - 16),
                    6, thumbH + (title[1].length() > 0 ? lineH * 4 : lineH * 3));
            card.dispose();
        }
    }

    private void drawThumb(Graphics g, final VideoResult v, int x, int y, int w, int h) {
        g.setColor(Color.darkGray);
        g.fillRect(x, y, w, h);
        Image img = thumbs.get(v.videoId);
        if (img != null) {
            int iw = img.getWidth(this), ih = img.getHeight(this);
            if (iw > 0 && ih > 0) {
                double sx = (double) w / (double) iw;
                double sy = (double) h / (double) ih;
                double s = Math.min(sx, sy);
                int dw = Math.max(1, (int) (iw * s));
                int dh = Math.max(1, (int) (ih * s));
                g.drawImage(img, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh, this);
            }
        } else {
            g.setColor(Color.lightGray);
            g.setFont(new Font("Dialog", Font.PLAIN, fontSize));
            g.drawString("preview", x + 4, y + h / 2);
        }
        requestThumb(v);
    }

    private void requestThumb(final VideoResult v) {
        if (thumbs.containsKey(v.videoId) || thumbLoading.containsKey(v.videoId)) return;
        thumbLoading.put(v.videoId, Boolean.TRUE);
        Thread t = new Thread(new Runnable() {
            public void run() {
                Image img = null;
                try {
                    URL u = new URL("http://img.youtube.com/vi/" + v.videoId + "/mqdefault.jpg");
                    URLConnection c = u.openConnection();
                    c.setConnectTimeout(5000);
                    c.setReadTimeout(7000);
                    InputStream in = c.getInputStream();
                    try {
                        img = ImageIO.read(in);
                    } finally {
                        try { in.close(); } catch (Exception ignored2) {}
                    }
                } catch (Exception ignored) {
                    /* Thumbnail failure must never affect playback or selection. */
                }
                synchronized (JTubeCanvas.this) {
                    thumbLoading.remove(v.videoId);
                    if (img != null) putThumb(v.videoId, img);
                }
                updatePreviewStatus();
            }
        }, "notPipe-thumb");
        t.setDaemon(true);
        t.start();
    }

    private synchronized void putThumb(String id, Image img) {
        if (thumbs.containsKey(id)) return;
        thumbs.put(id, img);
        thumbOrder.addLast(id);
        while (thumbOrder.size() > thumbCacheLimit()) {
            String old = thumbOrder.removeFirst();
            thumbs.remove(old);
        }
    }

    private int thumbCacheLimit() {
        int mode = LayoutMode.detect(getWidth(), getHeight());
        int visible = visibleCount(mode, getWidth(), getHeight());
        return Math.min(results.size(), visible) + 2;
    }

    private void updatePreviewStatus() {
        if (playbackBusy) return;
        int mode = LayoutMode.detect(getWidth(), getHeight());
        int end = visibleEnd(mode, getHeight());
        int wanted = Math.max(0, end - firstVisible);
        int ready = 0;
        int loading = 0;
        synchronized (this) {
            for (int i = firstVisible; i < end; i++) {
                VideoResult video = results.get(i);
                if (thumbs.containsKey(video.videoId)) ready++;
                else if (thumbLoading.containsKey(video.videoId)) loading++;
            }
        }
        if (wanted > 0) {
            setMessage("PREVIEWS: " + ready + "/" + wanted
                    + (loading > 0 ? " LOADING" : " READY"));
        } else {
            repaint();
        }
    }

    private void drawSelection(Graphics g, int i, int x, int y, int w, int h) {
        if (i == selected) {
            g.setColor(new Color(45, 70, 105));
            g.fillRect(x, y, w, h);
        }
    }

    private String formatDuration(int totalSeconds) {
        if (totalSeconds < 0) return "--:--";
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return hours + ":" + twoDigits(minutes) + ":" + twoDigits(seconds);
        }
        return minutes + ":" + twoDigits(seconds);
    }

    private void drawNumberDuration(Graphics g, VideoResult video, int x, int baseline) {
        String number = video.number + ".";
        g.setFont(new Font("Dialog", Font.BOLD, fontSize));
        g.drawString(number, x, baseline);
        int durationX = x + g.getFontMetrics().stringWidth(number) + 8;
        g.setFont(new Font("Dialog", Font.PLAIN, fontSize));
        g.drawString(formatDuration(video.durationSeconds), durationX, baseline);
    }

    private String twoDigits(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }

    private String[] wrapTwoLines(String text, FontMetrics metrics, int maxWidth) {
        String value = text == null ? "" : text.trim();
        if (metrics.stringWidth(value) <= maxWidth) return new String[] { value, "" };

        int cut = value.length();
        while (cut > 0 && metrics.stringWidth(value.substring(0, cut)) > maxWidth) cut--;
        if (cut <= 0) return new String[] { fitText(value, metrics, maxWidth), "" };

        int space = value.lastIndexOf(' ', cut);
        if (space > 0) cut = space;
        String first = value.substring(0, cut).trim();
        String second = value.substring(cut).trim();
        return new String[] { first, fitText(second, metrics, maxWidth) };
    }

    private String shorten(String s, int n) {
        if (s == null) return "";
        if (s.length() <= n) return s;
        if (n < 4) return s.substring(0, n);
        return s.substring(0, n - 3) + "...";
    }

    private void writeSelection(final VideoResult v) {
        if (playback == null) {
            message = "SELECT " + v.number + ": " + v.videoId;
            repaint();
            return;
        }
        if (playbackBusy) {
            message = "PLAYBACK BUSY";
            repaint();
            return;
        }
        playbackBusy = true;
        message = "OPENING " + v.number + " VIA " + v.backend + "...";
        repaint();
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    playback.play(v);
                    message = "VIDEO LAUNCHED: " + v.number;
                } catch (Exception ex) {
                    message = "PLAYBACK FAILED: " + ex.getMessage();
                } finally {
                    playbackBusy = false;
                    repaint();
                }
            }
        }, "NOS-Pipe-playback");
        t.setDaemon(true);
        t.start();
    }

    private void move(int delta) {
        if (results.isEmpty()) return;
        selected += delta;
        if (selected < 0) selected = results.size() - 1;
        if (selected >= results.size()) selected = 0;
        ensureVisible();
        message = "SELECTED " + results.get(selected).number;
        repaint();
    }

    private void page(int delta) {
        if (results.isEmpty()) return;
        int mode = LayoutMode.detect(getWidth(), getHeight());
        int count = visibleCount(mode, getWidth(), getHeight());
        selected += delta * count;
        if (selected < 0) selected = 0;
        if (selected >= results.size()) selected = results.size() - 1;
        ensureVisible();
        message = "SELECTED " + results.get(selected).number;
        repaint();
    }

    private void choose() {
        if (results.isEmpty()) return;
        VideoResult v = results.get(selected);
        message = "SELECT " + v.number + ": " + v.videoId;
        writeSelection(v);
        repaint();
    }

    private void paintControls(Graphics g, int w, int h, int mode) {
        if (mode == LayoutMode.ULTRAWIDE) {
            int x0 = w - CONTROL_SIZE - CONTROL_MARGIN;
            int y = Math.max(contentTop(), (h - (CONTROL_SIZE * 4 + CONTROL_GAP * 3)) / 2);
            drawControl(g, CONTROL_PLAY, x0, y, "");
            drawControl(g, CONTROL_SEARCH, x0, y + CONTROL_SIZE + CONTROL_GAP, "");
            drawControl(g, CONTROL_SETTINGS, x0, y + 2 * (CONTROL_SIZE + CONTROL_GAP), "");
            drawControl(g, CONTROL_EXIT, x0, y + 3 * (CONTROL_SIZE + CONTROL_GAP), "");
        } else {
            int total = CONTROL_SIZE * 4 + CONTROL_GAP * 3;
            int x = Math.max(CONTROL_MARGIN, (w - total) / 2);
            int y = h - CONTROL_BAR_H + CONTROL_MARGIN;
            drawControl(g, CONTROL_PLAY, x, y, "");
            drawControl(g, CONTROL_SEARCH, x + CONTROL_SIZE + CONTROL_GAP, y, "");
            drawControl(g, CONTROL_SETTINGS, x + 2 * (CONTROL_SIZE + CONTROL_GAP), y, "");
            drawControl(g, CONTROL_EXIT, x + 3 * (CONTROL_SIZE + CONTROL_GAP), y, "");
        }
    }

    private void drawControl(Graphics g, int type, int x, int y, String unused) {
        if (type == activeControl) {
            g.setColor(new Color(70, 100, 145));
            g.fillRoundRect(x, y, CONTROL_SIZE, CONTROL_SIZE, 8, 8);
            g.setColor(Color.white);
            g.drawRoundRect(x, y, CONTROL_SIZE, CONTROL_SIZE, 8, 8);
        } else {
            g.setColor(new Color(38, 38, 38));
            g.fillRoundRect(x, y, CONTROL_SIZE, CONTROL_SIZE, 8, 8);
            g.setColor(Color.lightGray);
            g.drawRoundRect(x, y, CONTROL_SIZE, CONTROL_SIZE, 8, 8);
        }
        int cx = x + CONTROL_SIZE / 2;
        int cy = y + CONTROL_SIZE / 2;

        if (type == CONTROL_PLAY) {
            Polygon p = new Polygon();
            p.addPoint(cx - 7, cy - 10);
            p.addPoint(cx - 7, cy + 10);
            p.addPoint(cx + 10, cy);
            g.fillPolygon(p);
        } else if (type == CONTROL_SEARCH) {
            g.drawOval(cx - 9, cy - 9, 15, 15);
            g.drawLine(cx + 2, cy + 2, cx + 10, cy + 10);
        } else if (type == CONTROL_SETTINGS) {
            g.drawOval(cx - 8, cy - 8, 16, 16);
            g.fillRect(cx - 2, cy - 13, 4, 7);
            g.fillRect(cx - 2, cy + 6, 4, 7);
            g.fillRect(cx - 13, cy - 2, 7, 4);
            g.fillRect(cx + 6, cy - 2, 7, 4);
            g.fillOval(cx - 3, cy - 3, 6, 6);
        } else {
            g.drawLine(cx - 9, cy, cx + 9, cy);
            g.drawLine(cx + 9, cy, cx + 2, cy - 7);
            g.drawLine(cx + 9, cy, cx + 2, cy + 7);
            g.drawLine(cx - 9, cy, cx - 9, cy - 8);
        }
    }

    private int controlAt(int x, int y) {
        int w = getWidth(), h = getHeight();
        int mode = LayoutMode.detect(w, h);
        if (mode == LayoutMode.ULTRAWIDE) {
            int x0 = w - CONTROL_SIZE - CONTROL_MARGIN;
            int y0 = Math.max(contentTop(), (h - (CONTROL_SIZE * 4 + CONTROL_GAP * 3)) / 2);
            if (x >= x0 && x < x0 + CONTROL_SIZE) {
                for (int i = 0; i < 4; i++) {
                    int yy = y0 + i * (CONTROL_SIZE + CONTROL_GAP);
                    if (y >= yy && y < yy + CONTROL_SIZE) return i;
                }
            }
        } else {
            int total = CONTROL_SIZE * 4 + CONTROL_GAP * 3;
            int x0 = Math.max(CONTROL_MARGIN, (w - total) / 2);
            int y0 = h - CONTROL_BAR_H + CONTROL_MARGIN;
            if (y >= y0 && y < y0 + CONTROL_SIZE) {
                for (int i = 0; i < 4; i++) {
                    int xx = x0 + i * (CONTROL_SIZE + CONTROL_GAP);
                    if (x >= xx && x < xx + CONTROL_SIZE) return i;
                }
            }
        }
        return -1;
    }

    private void activateControl(int control) {
        if (control == CONTROL_PLAY) {
            choose();
        } else if (control == CONTROL_SEARCH) {
            showSearchDialog();
        } else if (control == CONTROL_SETTINGS) {
            openSettings();
        } else if (control == CONTROL_EXIT) {
            System.exit(0);
        }
    }

    private void showSearchDialog() {
        final Dialog d = new Dialog((Frame) getParent(), "Search", true);
        d.setLayout(new FlowLayout());
        d.add(new Label("Search:"));
        final TextField field = new TextField(query, 28);
        d.add(field);
        Button ok = new Button("OK");
        Button cancel = new Button("Cancel");
        d.add(ok);
        d.add(cancel);
        final KeyEventDispatcher escape = new KeyEventDispatcher() {
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (d.isShowing() && e.getID() == KeyEvent.KEY_PRESSED
                        && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    closeSearchDialog(d, this);
                    return true;
                }
                return false;
            }
        };
        ActionListener submit = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                submitSearch(d, field, escape);
            }
        };
        ok.addActionListener(submit);
        field.addActionListener(submit);
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { closeSearchDialog(d, escape); }
        });
        d.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { closeSearchDialog(d, escape); }
        });
        d.setSize(360, 100);
        d.setLocationRelativeTo(this);
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(escape);
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                field.requestFocus();
                field.selectAll();
            }
        });
        d.setVisible(true);
    }

    private void submitSearch(final Dialog dialog, TextField field,
                              KeyEventDispatcher escape) {
        String q = field.getText();
        if (q == null || q.trim().length() == 0) {
            closeSearchDialog(dialog, escape);
            repaint();
            return;
        }

        final String newQuery = q.trim();
        query = newQuery;
        closeSearchDialog(dialog, escape);
        message = "SEARCHING: " + newQuery;
        repaint();
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Gateway[] gs = GatewayConfig.load(AppPaths.configFile());
                    SearchCoordinator sc = new SearchCoordinator(gs, JTubeCanvas.this);
                    List fresh = sc.search(newQuery);
                    setResults(fresh);
                    message = "READY: " + results.size() + " RESULTS";
                } catch (Exception ex) {
                    message = "SEARCH FAILED: " + ex.getMessage();
                }
                repaint();
            }
        }, "NOS-Pipe-search");
        t.setDaemon(true);
        t.start();
    }

    private void closeSearchDialog(Dialog dialog, KeyEventDispatcher escape) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .removeKeyEventDispatcher(escape);
        dialog.dispose();
    }

    private void openSettings() {
        try {
            SettingsDialog d = new SettingsDialog((Frame) getParent(), AppPaths.configFile(), this);
            d.setLocationRelativeTo(this);
            d.setVisible(true);
            message = "SETTINGS READY";
        } catch (Exception e) {
            message = "SETTINGS FAILED: " + e.getMessage();
        }
        repaint();
    }

    public int getFontSize() { return fontSize; }

    public void setMessage(String s) { message = s == null ? "" : s; repaint(); }

    public void status(String text) { setMessage(text); }

    public void setResults(List fresh) {
        List copy = new ArrayList();
        if (fresh != null) copy.addAll(fresh);
        results = Collections.unmodifiableList(copy);
        selected = 0;
        firstVisible = 0;
        synchronized (this) {
            thumbs.clear();
            thumbOrder.clear();
            thumbLoading.clear();
        }
        repaint();
    }

    public void applySettings(int newFontSize, int newVideoHeight) {
        if (newFontSize >= 8 && newFontSize <= 24) fontSize = newFontSize;
        if (newVideoHeight > 0) videoHeight = newVideoHeight;
        if (playback != null) playback.setPreferredVideoHeight(videoHeight);
        repaint();
    }

    private void moveControl(int delta) {
        activeControl = (activeControl + delta + 4) % 4;
        message = "CONTROL " + (activeControl + 1);
        repaint();
    }

    private void activateActiveControl() {
        activateControl(activeControl);
    }

    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        /* Direct hardware/function-key access to the four controls. */
        if (code == KeyEvent.VK_F1) { activeControl = CONTROL_PLAY; activateActiveControl(); return; }
        if (code == KeyEvent.VK_F2) { activeControl = CONTROL_SEARCH; activateActiveControl(); return; }
        if (code == KeyEvent.VK_F3) { activeControl = CONTROL_SETTINGS; activateActiveControl(); return; }
        if (code == KeyEvent.VK_ESCAPE) { activateControl(CONTROL_EXIT); return; }

        int mode = LayoutMode.detect(getWidth(), getHeight());
        if (mode == LayoutMode.ULTRAWIDE) {
            if (code == KeyEvent.VK_DOWN) { moveControl(1); return; }
            if (code == KeyEvent.VK_UP) { moveControl(-1); return; }
            if (code == KeyEvent.VK_RIGHT) { move(1); return; }
            if (code == KeyEvent.VK_LEFT) { move(-1); return; }
        } else {
            if (code == KeyEvent.VK_RIGHT) { moveControl(1); return; }
            if (code == KeyEvent.VK_LEFT) { moveControl(-1); return; }
        }

        switch (code) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
            case KeyEvent.VK_NUMPAD8:
            case KeyEvent.VK_8: move(-1); break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
            case KeyEvent.VK_NUMPAD2:
            case KeyEvent.VK_2: move(1); break;
            case KeyEvent.VK_PAGE_UP: page(-1); break;
            case KeyEvent.VK_PAGE_DOWN: page(1); break;
            case KeyEvent.VK_HOME: selected = 0; ensureVisible(); repaint(); break;
            case KeyEvent.VK_END: selected = Math.max(0, results.size() - 1); ensureVisible(); repaint(); break;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_NUMPAD5:
            case KeyEvent.VK_5:
                /* Universal action key: execute the currently selected GUI control. */
                activateActiveControl();
                break;
        }
    }

    public void mousePressed(MouseEvent e) {
        int control = controlAt(e.getX(), e.getY());
        if (control >= 0) {
            activeControl = control;
            activateControl(control);
            requestFocus();
            repaint();
            return;
        }

        int w = getWidth(), h = getHeight(), mode = LayoutMode.detect(w, h);
        int top = contentTop();
        int rowH;
        if (mode == LayoutMode.PORTRAIT) rowH = portraitRowHeight();
        else if (mode == LayoutMode.ULTRAWIDE) rowH = 32;
        else rowH = landscapeRowHeight();

        int i;
        if (mode == LayoutMode.ULTRAWIDE) {
            int right = w - CONTROL_SIZE - CONTROL_MARGIN * 2;
            int usableW = Math.max(1, right - 6);
            int count = visibleCount(LayoutMode.ULTRAWIDE, w, h);
            int cardW = Math.max(130, usableW / count);
            i = firstVisible + (e.getX() - 4) / cardW;
            if (e.getX() < 4) i = -1;
        } else {
            i = firstVisible + (e.getY() - top) / rowH;
        }
        if (i >= 0 && i < results.size() && (mode == LayoutMode.ULTRAWIDE ? e.getY() >= top : e.getY() >= top)) {
            selected = i;
            if (e.getClickCount() >= 2) choose();
            else {
                message = "SELECTED " + results.get(selected).number;
                repaint();
            }
        }
        requestFocus();
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        move(e.getWheelRotation() > 0 ? 1 : -1);
    }

    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}
    public void mouseClicked(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
}
