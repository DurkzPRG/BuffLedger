package com.durkz.buffledger.buff;

public record ActiveBuff(
        String effectId,
        String displayName,
        long remainingMs,
        boolean infinite,
        boolean debuff
) {

    public String hudToken() {
        char sign = debuff ? '-' : '+';
        return sign + displayName + ' ' + BuffDurationFormat.format(remainingMs, infinite);
    }
}
