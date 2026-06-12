package com.durkz.buffledger.hud;

import com.durkz.buffledger.BuffLedgerPlugin;
import com.durkz.buffledger.buff.ActiveBuff;
import com.durkz.buffledger.buff.BuffSnapshotService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BuffLedgerHudService {

    private static final long REFRESH_SECONDS = 1L;
    private static final int MAX_EFFECT_LINES = 3;

    private final BuffSnapshotService snapshots;
    private final Map<UUID, BuffLedgerHud> active = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> enabled = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> refreshTask;

    public BuffLedgerHudService(BuffSnapshotService snapshots) {
        this.snapshots = snapshots;
    }

    public BuffSnapshotService snapshots() {
        return snapshots;
    }

    public void start(BuffLedgerPlugin ignored) {
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "buffledger-hud");
            t.setDaemon(true);
            return t;
        });
        refreshTask = scheduler.scheduleAtFixedRate(
                this::refreshAll,
                REFRESH_SECONDS,
                REFRESH_SECONDS,
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        if (refreshTask != null) {
            refreshTask.cancel(false);
            refreshTask = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        active.clear();
        enabled.clear();
    }

    public void onDisconnect(UUID uuid) {
        if (uuid == null) {
            return;
        }
        active.remove(uuid);
        enabled.remove(uuid);
    }

    public boolean enable(PlayerRef playerRef, Store<EntityStore> store, Ref<EntityStore> ref) {
        if (playerRef == null || !playerRef.isValid()) {
            return false;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return false;
        }
        enabled.put(playerRef.getUuid(), true);
        BuffLedgerHud hud = active.computeIfAbsent(playerRef.getUuid(), id -> new BuffLedgerHud(playerRef));
        applySnapshot(playerRef, store, ref, hud, false);
        HudBridge.show(player, playerRef, hud);
        return true;
    }

    public void disable(PlayerRef playerRef, Store<EntityStore> store, Ref<EntityStore> ref) {
        if (playerRef == null) {
            return;
        }
        enabled.put(playerRef.getUuid(), false);
        active.remove(playerRef.getUuid());
        if (store == null || ref == null) {
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        HudBridge.hide(player, playerRef, BuffLedgerHud.KEY);
    }

    public boolean isEnabled(UUID uuid) {
        return Boolean.TRUE.equals(enabled.get(uuid));
    }

    public List<ActiveBuff> readActive(PlayerRef playerRef, Store<EntityStore> store, Ref<EntityStore> ref) {
        return snapshots.snapshot(playerRef, store, ref);
    }

    private void refreshAll() {
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            if (playerRef == null || !playerRef.isValid() || !isEnabled(playerRef.getUuid())) {
                continue;
            }
            UUID worldUuid = playerRef.getWorldUuid();
            if (worldUuid == null) {
                continue;
            }
            World world = Universe.get().getWorld(worldUuid);
            if (world == null) {
                continue;
            }
            world.execute(() -> refreshPlayer(playerRef));
        }
    }

    private void refreshPlayer(PlayerRef playerRef) {
        if (playerRef == null || !playerRef.isValid() || !isEnabled(playerRef.getUuid())) {
            return;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) {
            return;
        }
        BuffLedgerHud hud = active.get(playerRef.getUuid());
        if (hud == null) {
            return;
        }
        applySnapshot(playerRef, ref.getStore(), ref, hud, true);
    }

    private void applySnapshot(
            PlayerRef playerRef,
            Store<EntityStore> store,
            Ref<EntityStore> ref,
            BuffLedgerHud hud,
            boolean push
    ) {
        String[] lines = formatLines(snapshots.snapshot(playerRef, store, ref));
        hud.setLines(lines);
        if (push) {
            hud.pushUpdate();
        }
    }

    static String[] formatLines(List<ActiveBuff> buffs) {
        String[] lines = new String[BuffLedgerHud.LINE_COUNT];
        Arrays.fill(lines, "");

        if (buffs.isEmpty()) {
            lines[0] = "No active effects";
            return lines;
        }

        lines[0] = "Effects (" + buffs.size() + ")";
        if (buffs.size() <= MAX_EFFECT_LINES) {
            for (int i = 0; i < buffs.size(); i++) {
                lines[i + 1] = buffs.get(i).hudToken();
            }
            return lines;
        }

        lines[1] = buffs.get(0).hudToken();
        lines[2] = buffs.get(1).hudToken();
        lines[3] = "+" + (buffs.size() - 2) + " more";
        return lines;
    }
}
