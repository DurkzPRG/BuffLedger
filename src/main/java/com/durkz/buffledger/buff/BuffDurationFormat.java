package com.durkz.buffledger.buff;

import java.util.Locale;

final class BuffDurationFormat {

    private BuffDurationFormat() {}

    static String format(long remainingMs, boolean infinite) {
        if (infinite) {
            return "inf";
        }
        if (remainingMs <= 0L) {
            return "0s";
        }
        long totalSeconds = Math.max(1L, (remainingMs + 999L) / 1000L);
        if (totalSeconds < 60L) {
            return totalSeconds + "s";
        }
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }
}
