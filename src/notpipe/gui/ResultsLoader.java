package notpipe.gui;

import java.io.*;
import java.util.*;

public final class ResultsLoader {
    private ResultsLoader() {}

    public static List<VideoResult> load(File file) throws IOException {
        List<VideoResult> out = new ArrayList<VideoResult>();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), "UTF-8"));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0) continue;
                String[] p = line.split("\t", -1);
                if (p.length < 5) continue;
                try {
                    int n = Integer.parseInt(p[0].trim());
                    out.add(new VideoResult(n, p[4], p[3], p[1], p[2]));
                } catch (NumberFormatException ignored) {}
            }
        } finally {
            br.close();
        }
        return out;
    }
}
