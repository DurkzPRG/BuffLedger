package com.durkz.buffledger.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Optional integration with Buuz135 MultipleHUD (CurseForge: multiplehud).
 * v0.1: detection stub — wire reflection/API once MultipleHUD jar is in libs/.
 */
public final class MultipleHudSupport {

    private static volatile boolean probed;
    private static volatile boolean loaded;

    private MultipleHudSupport() {}

    public static boolean isLoaded() {
        if (!probed) {
            probed = true;
            try {
                Class.forName("com.buuz135.multiplehud.MultipleHUD");
                loaded = true;
            } catch (ClassNotFoundException ignored) {
                loaded = false;
            }
        }
        return loaded;
    }

    public static boolean register(PlayerRef playerRef, CustomUIHud hud) {
        // TODO: call MultipleHUD API (see underscore95 multiple-huds branch)
        return false;
    }

    public static boolean unregister(PlayerRef playerRef, String hudKey) {
        // TODO: call MultipleHUD API
        return false;
    }
}
