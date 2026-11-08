package ru.glavtoy.mobheist.util;

import java.util.Locale;

public final class Numbers {

    private static final String[] SUFFIXES = {"M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc",
            "UDc", "DDc", "TDc", "QaDc", "QiDc", "SxDc", "SpDc", "OcDc", "NoDc", "Vg"};
    private static final double THOUSAND = 1_000;
    private static final double MILLION = 1_000_000;

    private Numbers() {
    }

    public static String format(double value) {
        if (value < THOUSAND) {
            return value == Math.floor(value) ? String.valueOf((long) value) : String.format(Locale.US, "%.1f", value);
        }
        if (value < MILLION) {
            return String.format(Locale.US, "%,d", (long) value);
        }

        int index = (int) (Math.log10(value) / 3) - 2;
        if (index >= SUFFIXES.length) {
            return String.format(Locale.US, "%.2e", value);
        }
        String number = String.format(Locale.US, "%.2f", value / Math.pow(THOUSAND, index + 2));
        return number.replaceAll("\\.?0+$", "") + SUFFIXES[index];
    }
}
