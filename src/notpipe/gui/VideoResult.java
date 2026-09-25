package notpipe.gui;

public final class VideoResult {
    public final int number;
    public final String title;
    public final String author;
    public final String backend;
    public final String videoId;
    public final Gateway gateway;
    public final int durationSeconds;

    public VideoResult(int number, String title, String author,
                       String backend, String videoId) {
        this(number, title, author, backend, videoId, null);
    }

    public VideoResult(int number, String title, String author,
                       String backend, String videoId, Gateway gateway) {
        this(number, title, author, backend, videoId, gateway, -1);
    }

    public VideoResult(int number, String title, String author,
                       String backend, String videoId, Gateway gateway,
                       int durationSeconds) {
        this.number = number;
        this.title = title;
        this.author = author;
        this.backend = backend;
        this.videoId = videoId;
        this.gateway = gateway;
        this.durationSeconds = durationSeconds;
    }
}
