package notpipe.gui;

/** Receives short human-readable progress messages for the bottom UI line. */
public interface StatusListener {
    void status(String text);
}
