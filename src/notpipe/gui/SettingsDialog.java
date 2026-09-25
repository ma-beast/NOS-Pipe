package notpipe.gui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Choice;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Simple AWT-only user settings window. */
public final class SettingsDialog extends Dialog {
    private final File configFile;
    private final JTubeCanvas canvas;
    private final TextField fontField = new TextField(4);
    private final TextField videoField = new TextField(4);
    private final Choice playerMode = new Choice();
    private final TextField playerCommand = new TextField(28);
    private final Checkbox[] enabled = new Checkbox[20];
    private final TextField[] priority = new TextField[20];
    private final TextField[] type = new TextField[20];
    private final TextField[] api = new TextField[20];
    private final TextField[] name = new TextField[20];
    private final TextField[] proxy = new TextField[20];
    private boolean escapeInstalled;
    private final KeyEventDispatcher escapeDispatcher = new KeyEventDispatcher() {
        public boolean dispatchKeyEvent(KeyEvent e) {
            if (isShowing() && e.getID() == KeyEvent.KEY_PRESSED
                    && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                dispose();
                return true;
            }
            return false;
        }
    };

    public SettingsDialog(java.awt.Frame owner, File configFile, JTubeCanvas canvas) throws IOException {
        super(owner, "NOS-Pipe - Settings", true);
        this.configFile = configFile;
        this.canvas = canvas;
        build();
        load();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { dispose(); }
        });
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(escapeDispatcher);
        escapeInstalled = true;
    }

    private void build() {
        setLayout(new BorderLayout(8, 8));
        setBackground(Color.black);

        Panel top = new Panel(new GridLayout(3, 1));
        top.setBackground(Color.black);
        Label brand = new Label("NecronomicOS", Label.CENTER);
        brand.setForeground(Color.red);
        brand.setFont(new Font("Dialog", Font.BOLD, Math.max(12, canvas.getFontSize() + 2)));
        top.add(brand);
        Label authors = new Label("ChatGPT & Mikhail G. Freeman (ma_beast)", Label.CENTER);
        authors.setForeground(Color.yellow);
        authors.setFont(new Font("Dialog", Font.PLAIN, canvas.getFontSize()));
        top.add(authors);
        Label section = new Label("Settings", Label.CENTER);
        section.setForeground(Color.yellow);
        section.setFont(new Font("Dialog", Font.PLAIN, canvas.getFontSize()));
        top.add(section);
        add(top, BorderLayout.NORTH);

        Panel center = new Panel(new BorderLayout(4, 4));
        center.setBackground(Color.black);

        Panel basic = new Panel(new GridLayout(2, 1, 2, 2));
        basic.setBackground(Color.black);
        Panel common = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        common.setBackground(Color.black);
        Label f = label("Font size:");
        common.add(f);
        common.add(fontField);
        Label vh = label("Stream quality (p):");
        common.add(vh);
        common.add(videoField);
        Label pm = label("Player:");
        common.add(pm);
        playerMode.add("auto");
        playerMode.add("mplayer");
        playerMode.add("system");
        playerMode.add("custom");
        common.add(playerMode);
        basic.add(common);
        Panel custom = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        custom.setBackground(Color.black);
        Label pc = label("Custom command:");
        custom.add(pc);
        custom.add(playerCommand);
        basic.add(custom);
        center.add(basic, BorderLayout.NORTH);

        Panel grid = new Panel(new GridLayout(21, 6, 2, 2));
        grid.setBackground(Color.black);
        addHeader(grid, "ON");
        addHeader(grid, "PRIORITY");
        addHeader(grid, "TYPE");
        addHeader(grid, "API");
        addHeader(grid, "NAME");
        addHeader(grid, "PROXY");
        for (int i = 0; i < 20; i++) {
            enabled[i] = new Checkbox();
            enabled[i].setBackground(Color.black);
            priority[i] = new TextField(5);
            type[i] = new TextField(7);
            api[i] = new TextField(18);
            name[i] = new TextField(14);
            proxy[i] = new TextField(18);
            grid.add(enabled[i]);
            grid.add(priority[i]);
            grid.add(type[i]);
            grid.add(api[i]);
            grid.add(name[i]);
            grid.add(proxy[i]);
        }
        ScrollPane scroll = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        scroll.add(grid);
        scroll.setPreferredSize(new Dimension(760, 300));
        center.add(scroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        Panel bottom = new Panel(new BorderLayout());
        bottom.setBackground(Color.black);
        Label note = new Label("NOS-Pipe — Universal legacy-friendly YouTube frontend", Label.CENTER);
        note.setForeground(Color.cyan);
        note.setFont(new Font("Dialog", Font.PLAIN, Math.max(10, canvas.getFontSize() - 1)));
        bottom.add(note, BorderLayout.NORTH);

        Panel buttons = new Panel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        buttons.setBackground(Color.black);
        Button save = new Button("Save");
        Button cancel = new Button("Cancel");
        buttons.add(save);
        buttons.add(cancel);
        bottom.add(buttons, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    saveConfig();
                    dispose();
                } catch (Exception ex) {
                    canvas.setMessage("SETTINGS FAILED: " + ex.getMessage());
                }
            }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });

        setSize(820, 500);
        setResizable(true);
    }

    private Label label(String s) {
        Label l = new Label(s);
        l.setForeground(Color.yellow);
        l.setBackground(Color.black);
        return l;
    }

    private void addHeader(Panel p, String s) {
        Label l = new Label(s, Label.CENTER);
        l.setForeground(Color.lightGray);
        l.setBackground(Color.black);
        p.add(l);
    }

    private void load() throws IOException {
        Properties p = new Properties();
        InputStream in = new FileInputStream(configFile);
        try { p.load(in); } finally { in.close(); }
        fontField.setText(p.getProperty("font.size", "14"));
        videoField.setText(p.getProperty("video.height", "360"));
        selectPlayerMode(p.getProperty("player.mode", "auto"));
        playerCommand.setText(p.getProperty("player.command", ""));
        for (int i = 0; i < 20; i++) {
            String n = i < 9 ? "0" + (i + 1) : "" + (i + 1);
            enabled[i].setState("true".equalsIgnoreCase(p.getProperty("GATEWAY_" + n + "_ENABLED", "false")));
            priority[i].setText(p.getProperty("GATEWAY_" + n + "_PRIORITY", ""));
            type[i].setText(p.getProperty("GATEWAY_" + n + "_TYPE", "INVIDIOUS"));
            api[i].setText(p.getProperty("GATEWAY_" + n + "_API", ""));
            name[i].setText(p.getProperty("GATEWAY_" + n + "_NAME", "Gateway " + (i + 1)));
            proxy[i].setText(p.getProperty("GATEWAY_" + n + "_PROXY", ""));
        }
    }

    private void saveConfig() throws IOException {
        int font = parse(fontField.getText(), 14);
        if (font < 8 || font > 24) font = 14;
        int video = parse(videoField.getText(), 360);
        if (video < 144) video = 144;

        Properties p = new Properties();
        InputStream in = new FileInputStream(configFile);
        try { p.load(in); } finally { in.close(); }
        p.setProperty("font.size", Integer.toString(font));
        p.setProperty("video.height", Integer.toString(video));
        p.setProperty("player.mode", playerMode.getSelectedItem());
        p.setProperty("player.command", playerCommand.getText().trim());
        for (int i = 0; i < 20; i++) {
            String n = i < 9 ? "0" + (i + 1) : "" + (i + 1);
            p.setProperty("GATEWAY_" + n + "_ENABLED", Boolean.toString(enabled[i].getState()));
            p.setProperty("GATEWAY_" + n + "_PRIORITY", priority[i].getText().trim());
            p.setProperty("GATEWAY_" + n + "_TYPE", type[i].getText().trim());
            p.setProperty("GATEWAY_" + n + "_API", api[i].getText().trim());
            p.setProperty("GATEWAY_" + n + "_NAME", name[i].getText().trim());
            p.setProperty("GATEWAY_" + n + "_PROXY", proxy[i].getText().trim());
        }
        FileOutputStream out = new FileOutputStream(configFile);
        try {
            writePlayerExamples(out);
            p.store(out, "NOS-Pipe configuration");
        } finally { out.close(); }
        canvas.applySettings(font, video);
    }

    private void writePlayerExamples(FileOutputStream out) throws IOException {
        String lines =
                "# Player modes: auto, mplayer, system, custom.\r\n"
                + "# VLC standard Windows path, muxed stream:\r\n"
                + "# player.command=\"C:\\Program Files\\VideoLAN\\VLC\\vlc.exe\" {video}\r\n"
                + "# VLC standard Windows path, separate video and audio streams:\r\n"
                + "# player.command=\"C:\\Program Files\\VideoLAN\\VLC\\vlc.exe\" --input-slave={audio} {video}\r\n";
        out.write(lines.getBytes("ISO-8859-1"));
    }

    private void selectPlayerMode(String mode) {
        String wanted = mode == null ? "auto" : mode.trim().toLowerCase();
        for (int i = 0; i < playerMode.getItemCount(); i++) {
            if (playerMode.getItem(i).equals(wanted)) {
                playerMode.select(i);
                return;
            }
        }
        playerMode.select(0);
    }

    private int parse(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    public void dispose() {
        if (escapeInstalled) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .removeKeyEventDispatcher(escapeDispatcher);
            escapeInstalled = false;
        }
        super.dispose();
    }
}
