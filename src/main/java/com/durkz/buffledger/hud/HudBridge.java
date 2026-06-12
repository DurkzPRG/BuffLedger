package com.durkz.buffledger.hud;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Native HUD slot via HudManager.addCustomHud. Optional MultipleHUD bridge when present.
 */
public final class HudBridge {

    private HudBridge() {}

    public static void show(Player player, PlayerRef playerRef, CustomUIHud hud) {
        if (player == null || playerRef == null || hud == null) {
            return;
        }
        if (MultipleHudSupport.isLoaded() && MultipleHudSupport.register(playerRef, hud)) {
            return;
        }
        player.getHudManager().addCustomHud(playerRef, hud);
    }

    public static void hide(Player player, PlayerRef playerRef, String hudKey) {
        if (player == null || playerRef == null || hudKey == null) {
            return;
        }
        if (MultipleHudSupport.isLoaded() && MultipleHudSupport.unregister(playerRef, hudKey)) {
            return;
        }
        player.getHudManager().removeCustomHud(playerRef, hudKey);
    }
}
