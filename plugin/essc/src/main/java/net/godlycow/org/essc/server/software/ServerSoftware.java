package net.godlycow.org.essc.server.software;

public enum ServerSoftware {

    FOLIA,
    PAPER,
    SPIGOT;

    private static final ServerSoftware DETECTED = detect();

    private static ServerSoftware detect() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return FOLIA;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return PAPER;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("io.papermc.paper.configuration.GlobalConfiguration");
            return PAPER;
        } catch (ClassNotFoundException ignored) {
        }
        return SPIGOT;
    }

    public static ServerSoftware get() {
        return DETECTED;
    }

    public static boolean isFolia() {
        return DETECTED == FOLIA;
    }

    public static boolean isPaper() {
        return DETECTED == PAPER || DETECTED == FOLIA;
    }
}
