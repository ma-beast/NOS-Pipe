package notpipe.gui;

import java.io.File;
import java.util.List;

public final class LiveSearchProbe {
    public static void main(String[] args) throws Exception {
        String q = args.length == 0 ? "cats" : args[0];
        Gateway[] gs = GatewayConfig.load(AppPaths.configFile());
        System.out.println("NOS-Pipe 0.2 LiveSearchProbe");
        System.out.println("Search: " + q);
        System.out.println("Gateway order:");
        for (int i = 0; i < gs.length; i++) System.out.println("  " + (i + 1) + ". " + gs[i]);
        List r = new SearchCoordinator(gs).search(q);
        System.out.println();
        System.out.println("Results: " + r.size());
        for (int i = 0; i < r.size(); i++) {
            VideoResult v = (VideoResult)r.get(i);
            System.out.println(v.number + ". [" + v.backend + "] " + v.title +
                    " [" + v.videoId + "] by " + v.author);
        }
    }
}
