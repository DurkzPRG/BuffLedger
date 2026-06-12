package com.durkz.buffledger.buff;

public record ActiveBuff(
        String effectId,
        String displayName,
        long remainingMs,
        boolean infinite,
        boolean debuff,
        int stacks
) {

    public ActiveBuff(String effectId, String displayName, long remainingMs, boolean infinite, boolean debuff) {
        this(effectId, displayName, remainingMs, infinite, debuff, 1);
    }

    public String hudToken() {
        char sign = debuff ? '-' : '+';
        String stackSuffix = stacks > 1 ? " x" + stacks : "";
        return sign + displayName + stackSuffix + ' ' + BuffDurationFormat.format(remainingMs, infinite);
    }
}
