package com.durkz.buffledger;

import com.durkz.buffledger.buff.BuffSnapshotService;
import com.durkz.buffledger.command.BuffCommand;
import com.durkz.buffledger.hud.BuffLedgerHudService;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class BuffLedgerPlugin extends JavaPlugin {

    private static BuffLedgerPlugin instance;
    private BuffSnapshotService snapshotService;
    private BuffLedgerHudService hudService;

    public BuffLedgerPlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static BuffLedgerPlugin getInstance() {
        return instance;
    }

    public BuffSnapshotService snapshots() {
        return snapshotService;
    }

    public BuffLedgerHudService hudService() {
        return hudService;
    }

    @Override
    protected void setup() {
        super.setup();
        snapshotService = new BuffSnapshotService();
        hudService = new BuffLedgerHudService(snapshotService);
        getCommandRegistry().registerCommand(new BuffCommand());
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, e -> {
            if (e.getPlayerRef() != null) {
                hudService.onDisconnect(e.getPlayerRef().getUuid());
            }
        });
        getLogger().atInfo().log("BuffLedger %s setup.", getManifest().getVersion());
    }

    @Override
    protected void start() {
        super.start();
        if (hudService != null) {
            hudService.start(this);
        }
    }

    @Override
    protected void shutdown() {
        if (hudService != null) {
            hudService.stop();
            hudService = null;
        }
        snapshotService = null;
        instance = null;
        super.shutdown();
    }
}
