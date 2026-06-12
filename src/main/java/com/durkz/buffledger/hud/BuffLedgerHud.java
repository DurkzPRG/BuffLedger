package com.durkz.buffledger.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class BuffLedgerHud extends CustomUIHud {

    public static final String KEY = "durkz:buff_ledger";
    public static final int LINE_COUNT = 8;

    private final String[] lines = {"", "", "", "", "", "", "", ""};

    public BuffLedgerHud(PlayerRef playerRef) {
        super(playerRef, KEY, 40);
    }

    public void setLines(String... values) {
        for (int i = 0; i < LINE_COUNT; i++) {
            lines[i] = i < values.length ? sanitize(values[i]) : "";
        }
    }

    public void pushUpdate() {
        UICommandBuilder builder = new UICommandBuilder();
        build(builder);
        update(false, builder);
    }

    @Override
    protected void build(UICommandBuilder ui) {
        ui.append("Hud/Durkz_BuffLedger_Hud.ui");
        ui.set("#BlLine1.Text", lines[0]);
        ui.set("#BlLine2.Text", lines[1]);
        ui.set("#BlLine3.Text", lines[2]);
        ui.set("#BlLine4.Text", lines[3]);
        ui.set("#BlLine5.Text", lines[4]);
        ui.set("#BlLine6.Text", lines[5]);
        ui.set("#BlLine7.Text", lines[6]);
        ui.set("#BlLine8.Text", lines[7]);
    }

    private static String sanitize(String value) {
        return value == null ? "" : value;
    }
}
