package net.godlycow.org.essc.util;

public final class AnsiColorUtil {


    private static final String ESC   = "\033[";
    private static final String RESET = ESC + "0m";
    public static final String BOLD      = ESC + "1m";
    public static final String DIM       = ESC + "2m";
    public static final String COLOR_PRIMARY   = "#279CF5";
    public static final String COLOR_SECONDARY = "#C0F0FF";
    public static final String COLOR_ACCENT    = "#7B61FF";
    public static final String COLOR_SUCCESS   = "#00D97E";
    public static final String COLOR_WARNING   = "#F6C343";
    public static final String COLOR_DANGER    = "#E94B3C";
    public static final String COLOR_INFO      = "#36B5E6";
    public static final String COLOR_WHITE     = "#FFFFFF";
    public static final String COLOR_GRAY      = "#9B9B9B";

    private static final boolean ANSI_SUPPORTED = detectAnsi();

    private AnsiColorUtil() {}

    private static boolean detectAnsi() {
        if ("false".equalsIgnoreCase(System.getProperty("ansi.enabled")))
            return false;

        if ("true".equalsIgnoreCase(System.getProperty("ansi.enabled")))
            return true;

        String term = System.getenv("TERM");
        if ("dumb".equalsIgnoreCase(term))
            return false;

        if (System.getenv("CI") != null || System.getenv("GITHUB_ACTIONS") != null)
            return true;

        if (System.console() != null)
            return true;

        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("mac") || os.contains("win");
    }

    public static boolean isSupported() {
        return ANSI_SUPPORTED;
    }

    public static String fg(String hex) {
        if (!ANSI_SUPPORTED) return "";
        int[] rgb = parseHex(hex);
        return String.format("%s38;2;%d;%d;%dm", ESC, rgb[0], rgb[1], rgb[2]);
    }

    public static String bg(String hex) {
        if (!ANSI_SUPPORTED) return "";
        int[] rgb = parseHex(hex);
        return String.format("%s48;2;%d;%d;%dm", ESC, rgb[0], rgb[1], rgb[2]);
    }

    public static String reset() {
        return ANSI_SUPPORTED ? RESET : "";
    }

    public static String colorize(String hex, String text) {
        if (!ANSI_SUPPORTED) return text;
        return fg(hex) + text + RESET;
    }

    public static String styled(String hex, String text, String... styles) {
        if (!ANSI_SUPPORTED) return text;
        StringBuilder sb = new StringBuilder(fg(hex));
        for (String style : styles) sb.append(style);
        sb.append(text).append(RESET);
        return sb.toString();
    }

    public static String style(String text, String... styles) {
        if (!ANSI_SUPPORTED) return text;
        StringBuilder sb = new StringBuilder();
        for (String s : styles) sb.append(s);
        sb.append(text).append(RESET);
        return sb.toString();
    }

    public static String primary(String text) {
        return colorize(COLOR_PRIMARY,   text);
    }

    public static String success(String text) {
        return colorize(COLOR_SUCCESS,   text);
    }

    public static String warning(String text) {
        return colorize(COLOR_WARNING,   text);
    }

    public static String info(String text) {
        return colorize(COLOR_INFO,      text);
    }

    public static String gray(String text) {
        return colorize(COLOR_GRAY,      text);
    }

    public static String white(String text) {
        return colorize(COLOR_WHITE,     text);
    }

    public static String bold(String text) {
        return style(text, BOLD);
    }

    private static int[] parseHex(String hex) {
        if (hex == null) throw new IllegalArgumentException("Hex color cannot be null");
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        h = h.trim();
        if (h.length() != 6 || !h.matches("[0-9A-Fa-f]{6}")) {
            throw new IllegalArgumentException(
                    "Invalid hex color '" + hex + "': must be RRGGBB or #RRGGBB");
        }
        return new int[]{
                Integer.parseInt(h.substring(0, 2), 16),
                Integer.parseInt(h.substring(2, 4), 16),
                Integer.parseInt(h.substring(4, 6), 16)
        };
    }
}