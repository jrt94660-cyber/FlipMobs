package com.flipmobs.util;

import com.flipmobs.FlipMobs;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MoneyFormatter {
    private MoneyFormatter() {}

    public static String format(FlipMobs plugin, double amount) {
        int decimals = Math.max(0, Math.min(4, plugin.getConfig().getInt("format.decimals", 2)));
        DecimalFormat format = new DecimalFormat("#,##0." + "0".repeat(decimals),
                DecimalFormatSymbols.getInstance(Locale.US));
        double abs = Math.abs(amount);
        if (abs >= 1_000_000) return compact(format, amount / 1_000_000D)
                + plugin.getConfig().getString("format.millions-suffix", "M");
        if (abs >= 1_000) return compact(format, amount / 1_000D)
                + plugin.getConfig().getString("format.thousands-suffix", "K");
        return format.format(amount);
    }

    private static String compact(DecimalFormat format, double value) {
        return format.format(value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}