package notpipe.gui;

public final class Json {
    private Json() {}

    /** Returns top-level JSON objects. Used by the search result arrays. */
    public static String[] objects(String body) {
        return objectsAtAnyDepth(body, false, null);
    }

    /**
     * Returns object fragments at any nesting level which contain the given key.
     * This is intentionally small and dependency-free for old Java runtimes.
     */
    public static String[] objectsContainingKey(String body, String key) {
        return objectsAtAnyDepth(body, true, key);
    }

    /** Returns the direct object entries of a named JSON array. */
    public static String[] arrayObjects(String body, String key) {
        String needle = "\"" + key + "\"";
        int keyPos = body.indexOf(needle);
        if (keyPos < 0) return new String[0];
        int colon = body.indexOf(':', keyPos + needle.length());
        if (colon < 0) return new String[0];
        int start = body.indexOf('[', colon + 1);
        if (start < 0) return new String[0];

        boolean string = false, escaped = false;
        int depth = 0;
        for (int i = start; i < body.length(); i++) {
            char c = body.charAt(i);
            if (string) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') string = false;
                continue;
            }
            if (c == '"') string = true;
            else if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return objects(body.substring(start, i + 1));
            }
        }
        return new String[0];
    }

    private static String[] objectsAtAnyDepth(String body, boolean anyDepth, String key) {
        java.util.ArrayList out = new java.util.ArrayList();
        java.util.ArrayList starts = new java.util.ArrayList();
        java.util.ArrayList depths = new java.util.ArrayList();
        boolean str = false, esc = false;
        int depth = 0;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (str) {
                if (esc) esc = false;
                else if (c == '\\') esc = true;
                else if (c == '"') str = false;
                continue;
            }
            if (c == '"') { str = true; continue; }
            if (c == '{') {
                starts.add(Integer.valueOf(i));
                depths.add(Integer.valueOf(depth));
                depth++;
            } else if (c == '}') {
                if (starts.size() > 0) {
                    int last = starts.size() - 1;
                    int start = ((Integer) starts.remove(last)).intValue();
                    depths.remove(last);
                    String obj = body.substring(start, i + 1);
                    if (!anyDepth || depth == 1) {
                        if (!anyDepth || containsKey(obj, key)) out.add(obj);
                    } else if (containsKey(obj, key)) {
                        out.add(obj);
                    }
                }
                if (depth > 0) depth--;
            }
        }
        String[] a = new String[out.size()];
        out.toArray(a);
        return a;
    }

    private static boolean containsKey(String obj, String key) {
        return obj.indexOf("\"" + key + "\"") >= 0;
    }

    public static String string(String obj, String key) {
        String needle = "\"" + key + "\"";
        int p = obj.indexOf(needle);
        if (p < 0) return "";
        p = obj.indexOf(':', p + needle.length());
        if (p < 0) return "";
        p++;
        while (p < obj.length() && Character.isWhitespace(obj.charAt(p))) p++;
        if (p >= obj.length() || obj.charAt(p) != '"') return "";
        p++;
        StringBuffer b = new StringBuffer();
        boolean esc = false;
        while (p < obj.length()) {
            char c = obj.charAt(p++);
            if (esc) {
                if (c == 'n') b.append('\n');
                else if (c == 'r') b.append('\r');
                else if (c == 't') b.append('\t');
                else b.append(c);
                esc = false;
            } else if (c == '\\') esc = true;
            else if (c == '"') return b.toString();
            else b.append(c);
        }
        return "";
    }

    public static int integer(String obj, String key) {
        String needle = "\"" + key + "\"";
        int p = obj.indexOf(needle);
        if (p < 0) return -1;
        p = obj.indexOf(':', p + needle.length());
        if (p < 0) return -1;
        p++;
        while (p < obj.length() && Character.isWhitespace(obj.charAt(p))) p++;
        int start = p;
        if (p < obj.length() && (obj.charAt(p) == '-' || obj.charAt(p) == '+')) p++;
        while (p < obj.length() && Character.isDigit(obj.charAt(p))) p++;
        if (p == start) return -1;
        try { return Integer.parseInt(obj.substring(start, p)); }
        catch (Exception e) { return -1; }
    }

    public static boolean bool(String obj, String key, boolean def) {
        String needle = "\"" + key + "\"";
        int p = obj.indexOf(needle);
        if (p < 0) return def;
        p = obj.indexOf(':', p + needle.length());
        if (p < 0) return def;
        p++;
        while (p < obj.length() && Character.isWhitespace(obj.charAt(p))) p++;
        if (obj.startsWith("true", p)) return true;
        if (obj.startsWith("false", p)) return false;
        return def;
    }
}
