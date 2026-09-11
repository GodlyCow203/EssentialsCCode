package net.godlycow.org.essc.util;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class FormatUtil {

    private static final NavigableMap<Long, String> SUFFIXES = new TreeMap<>();
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    static {
        SUFFIXES.put(1_000L, "k");
        SUFFIXES.put(1_000_000L, "M");
        SUFFIXES.put(1_000_000_000L, "B");
        SUFFIXES.put(1_000_000_000_000L, "T");
        SUFFIXES.put(1_000_000_000_000_000L, "Q");
    }

    public static String formatNumber(double number) {
        return formatNumber((long) number);
    }

    public static String formatNumber(long number) {
        if (number == Long.MIN_VALUE) return formatNumber(Long.MIN_VALUE + 1);
        if (number < 0) return "-" + formatNumber(-number);
        if (number < 1000) return String.valueOf(number);

        Map.Entry<Long, String> entry = SUFFIXES.floorEntry(number);
        Long divideBy = entry.getKey();
        String suffix = entry.getValue();

        long truncated = number / (divideBy / 10);
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);

        if (hasDecimal) {
            return DECIMAL_FORMAT.format(truncated / 10d) + suffix;
        } else {
            return (truncated / 10) + suffix;
        }
    }

    public static String formatNumberWithDecimals(double number) {
        if (number == Double.MIN_VALUE) return formatNumberWithDecimals(Double.MIN_VALUE + 1);
        if (number < 0) return "-" + formatNumberWithDecimals(-number);
        if (number < 1000) return DECIMAL_FORMAT.format(number);

        long longValue = (long) number;
        Map.Entry<Long, String> entry = SUFFIXES.floorEntry(longValue);

        if (entry == null) return DECIMAL_FORMAT.format(number);

        Long divideBy = entry.getKey();
        String suffix = entry.getValue();

        double truncated = number / (divideBy / 10d);

        if (truncated < 100) {
            return DECIMAL_FORMAT.format(truncated / 10d) + suffix;
        } else {
            return ((long)(truncated / 10)) + suffix;
        }
    }

    public static String formatTime(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        if (seconds < 86400) return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        return (seconds / 86400) + "d " + ((seconds % 86400) / 3600) + "h";
    }

    public static String formatTimeShort(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m";
        if (seconds < 86400) return (seconds / 3600) + "h";
        return (seconds / 86400) + "d";
    }

    public static String formatPercentage(double value, double total) {
        if (total == 0) return "0%";
        return DECIMAL_FORMAT.format((value / total) * 100) + "%";
    }

    public static String formatCompact(double number) {
        if (Math.abs(number) < 1000) return DECIMAL_FORMAT.format(number);
        return formatNumberWithDecimals(number);
    }
}