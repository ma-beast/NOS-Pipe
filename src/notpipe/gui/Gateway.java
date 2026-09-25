package notpipe.gui;

public final class Gateway {
    public final String name;
    public final String type;
    public final String api;
    public final String proxy;
    public final int priority;
    public final boolean enabled;

    public Gateway(String name, String type, String api, String proxy,
                   int priority, boolean enabled) {
        this.name = name;
        this.type = type;
        this.api = api;
        this.proxy = proxy;
        this.priority = priority;
        this.enabled = enabled;
    }

    public String toString() {
        return name + " [" + type + "] " + api + " priority=" + priority;
    }
}
