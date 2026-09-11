package net.godlycow.org.essc.util;

import net.godlycow.org.essc.server.software.ServerSoftware;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;


public final class StartupBanner {

    private StartupBanner() {}

    public static void print(JavaPlugin plugin, Logger logger) {
        String version  = plugin.getDescription().getVersion();
        String software = ServerSoftware.get().name();
        String authors  = String.join(", ", plugin.getDescription().getAuthors());

        String p = AnsiColorUtil.COLOR_PRIMARY;
        String s = AnsiColorUtil.COLOR_SECONDARY;
        String g = AnsiColorUtil.COLOR_GRAY;
        String w = AnsiColorUtil.COLOR_WHITE;
        String ok= AnsiColorUtil.COLOR_SUCCESS;
        String wn= AnsiColorUtil.COLOR_WARNING;

        String swColor;
        String swLabel;
        switch (ServerSoftware.get()) {
            case FOLIA  -> { swColor = AnsiColorUtil.COLOR_ACCENT;  swLabel = "Folia  region-threaded"; }
            case PAPER  -> { swColor = ok;                          swLabel = "Paper"; }
            default     -> { swColor = wn;                          swLabel = "Spigot (limited support)"; }
        }

        String[] lines = {
                "",
                c(p, "  ███████╗███████╗███████╗ ██████╗"),
                c(p, "  ██╔════╝██╔════╝██╔════╝██╔════╝") + c(g, "  EssentialsC"),
                c(p, "  █████╗  ███████╗███████╗██║     ") + c(w, "  v" + version),
                c(p, "  ██╔══╝  ╚════██║╚════██║██║     ") + c(g, "  by " + authors),
                c(p, "  ███████╗███████║███████║╚██████╗"),
                c(p, "  ╚══════╝╚══════╝╚══════╝ ╚═════╝"),
                "",
                c(g, "  Platform  ") + AnsiColorUtil.styled(swColor, swLabel, AnsiColorUtil.BOLD),
                c(g, "  ANSI      ") + (AnsiColorUtil.isSupported()
                        ? AnsiColorUtil.success("supported")
                        : AnsiColorUtil.warning("not supported")),
                "",
        };

        for (String line : lines) {
            logger.info(line);
        }
    }

    private static String c(String hex, String text) {
        return AnsiColorUtil.colorize(hex, text);
    }
}