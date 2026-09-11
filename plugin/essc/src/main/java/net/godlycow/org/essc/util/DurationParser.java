package net.godlycow.org.essc.util;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

    public static final long PERMANENT = -1L;
    public static final long INVALID = 0L;
    private static final Pattern SEGMENT = Pattern.compile(
            "(\\d+)(y|mo|w|d|h|m|s)", Pattern.CASE_INSENSITIVE
    );

    private DurationParser() {}

    public static long parse(String input) {
        if (input == null || input.isBlank()) return INVALID;

        String s = input.trim().toLowerCase();

        if (s.equals("perm") || s.equals("permanent") || s.equals("forever") || s.equals("-1")) {
            return PERMANENT;
        }

        Matcher m = SEGMENT.matcher(s);
        long total = 0;
        boolean matched = false;

        while (m.find()) {
            matched = true;
            long value;
            try {
                value = Long.parseLong(m.group(1));
            } catch (NumberFormatException e) {
                return INVALID;
            }
            String unit = m.group(2).toLowerCase();
            try {
                long segment = switch (unit) {
                    case "y"  -> Math.multiplyExact(value, TimeUnit.DAYS.toMillis(365));
                    case "mo" -> Math.multiplyExact(value, TimeUnit.DAYS.toMillis(30));
                    case "w"  -> Math.multiplyExact(value, TimeUnit.DAYS.toMillis(7));
                    case "d"  -> Math.multiplyExact(value, TimeUnit.DAYS.toMillis(1));
                    case "h"  -> Math.multiplyExact(value, TimeUnit.HOURS.toMillis(1));
                    case "m"  -> Math.multiplyExact(value, TimeUnit.MINUTES.toMillis(1));
                    case "s"  -> Math.multiplyExact(value, TimeUnit.SECONDS.toMillis(1));
                    default   -> 0L;
                };
                total = Math.addExact(total, segment);
            } catch (ArithmeticException e) {
                return INVALID;
            }
        }

        if (matched) return total > 0 ? total : INVALID;

        try {
            long seconds = Long.parseLong(s);
            return seconds > 0 ? TimeUnit.SECONDS.toMillis(seconds) : INVALID;
        } catch (NumberFormatException e) {
            return INVALID;
        }
    }

    public static boolean isPermanent(long millis) {
        return millis == PERMANENT || millis <= 0;
    }

    public static String format(long millis) {
        if (millis <= 0) return "Permanent";

        long days    = TimeUnit.MILLISECONDS.toDays(millis);
        long hours   = TimeUnit.MILLISECONDS.toHours(millis)   % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis)  % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis)  % 60;

        if (days    > 0) return days + "d " + hours + "h";
        if (hours   > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    public static String formatShort(long millis) {
        if (millis <= 0) return "Permanent";

        long days    = TimeUnit.MILLISECONDS.toDays(millis);
        long hours   = TimeUnit.MILLISECONDS.toHours(millis)   % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis)  % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis)  % 60;

        if (days    > 0) return days + "d";
        if (hours   > 0) return hours + "h";
        if (minutes > 0) return minutes + "m";
        return seconds + "s";
    }

    public static String formatRemaining(long expiresAtMillis) {
        if (expiresAtMillis <= 0) return "Permanent";
        long remaining = expiresAtMillis - System.currentTimeMillis();
        if (remaining <= 0) return "Expired";
        return format(remaining);
    }

    public static String formatAgo(long timestampMillis) {
        long diff = System.currentTimeMillis() - timestampMillis;
        if (diff <= 0) return "Just now";

        long days    = TimeUnit.MILLISECONDS.toDays(diff);
        long hours   = TimeUnit.MILLISECONDS.toHours(diff)   % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;

        if (days    > 0) return days + "d ago";
        if (hours   > 0) return hours + "h ago";
        if (minutes > 0) return minutes + "m ago";
        return "Just now";
    }
}