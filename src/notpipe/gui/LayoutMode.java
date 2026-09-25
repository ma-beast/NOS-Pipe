package notpipe.gui;

public final class LayoutMode {
    public static final int LANDSCAPE = 1;
    public static final int PORTRAIT = 2;
    public static final int ULTRAWIDE = 3;

    private LayoutMode() {}

    public static int detect(int width, int height) {
        if (height <= 0) return LANDSCAPE;
        if ((double) width / (double) height >= (7.0 / 3.0)) return ULTRAWIDE;
        if (width >= height) return LANDSCAPE;
        return PORTRAIT;
    }

    public static String name(int mode) {
        if (mode == PORTRAIT) return "PORTRAIT";
        if (mode == ULTRAWIDE) return "ULTRAWIDE";
        return "LANDSCAPE";
    }
}
