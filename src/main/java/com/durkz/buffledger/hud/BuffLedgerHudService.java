package com.durkz.buffledger.hud;

import com.durkz.buffledger.buff.ActiveBuff;
import com.durkz.buffledger.buff.BuffSnapshotService;
import com.durkz.buffledger.prefs.BuffLedgerPrefs;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
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
    private static final int MAX_VISIBLE_EFFECTS = BuffLedgerHud.LINE_COUNT - 1;
    private static final int DISMISS_AFTER_EMPTY_TICKS = 8;
    private static final int DISMISS_AFTER_MORPH_CLEAR_TICKS = 20;
    private static final int DISMISS_AFTER_MULTI_CLEAR_TICKS = 15;

    private final BuffSnapshotService snapshots;
    private final BuffLedgerPrefs prefs;
    private final Map<UUID, BuffLedgerHud> active = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> tracking = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> emptyTicks = new ConcurrentHashMap<>();
    private final Map<UUID, List<ActiveBuff>> lastBuffs = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> refreshTask;

    public BuffLedgerHudService(BuffSnapshotService snapshots, BuffLedgerPrefs prefs) {
        this.snapshots = snapshots;
        this.prefs = prefs;
    }

    public BuffSnapshotService snapshots() {
        return snapshots;
    }

    public void start() {
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
        tracking.clear();
        emptyTicks.clear();
        lastBuffs.clear();
    }

    public void onDisconnect(PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }
        UUID uuid = playerRef.getUuid();
        active.remove(uuid);
        tracking.remove(uuid);
        emptyTicks.remove(uuid);
        lastBuffs.remove(uuid);
    }

    public void onPlayerReady(PlayerReadyEvent event) {
        if (event == null) {
            return;
        }
        Ref<EntityStore> entityRef = event.getPlayerRef();
        if (entityRef == null) {
            return;
        }
        Store<EntityStore> store = entityRef.getStore();
        if (store == null) {
            return;
        }

        EntityStore entityStore = store.getExternalData();
        World world = entityStore != null ? entityStore.getWorld() : null;
        if (world == null) {
            return;
        }

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null || !playerRef.isValid() || prefs.isOptedOut(playerRef.getUuid())) {
            return;
        }

        world.execute(() -> {
            if (!playerRef.isValid()) {
                return;
            }
            Ref<EntityStore> liveRef = playerRef.getReference();
            if (liveRef == null) {
                return;
            }
            startTracking(playerRef, liveRef.getStore(), liveRef);
        });
    }

    public boolean enable(PlayerRef playerRef, Store<EntityStore> store, Ref<EntityStore> ref) {
        if (playerRef == null || !playerRef.isValid()) {
            return false;
        }
        tracking.put(playerRef.getUuid(), true);
        prefs.setOptedOut(playerRef.getUuid(), false);
        emptyTicks.put(playerRef.getUuid(), 0);
        syncHud(playerRef, store, ref);
        return true;
    }

    public void disable(PlayerRef playerRef, Store<EntityStore> store, Ref<EntityStore> ref) {
        if (playerRef == null) {
            return;
        }
        UUID uuid = playerRef.getUuid();
        tracking.put(uuid, false);
        prefs.setOptedOut(uuid, true);
        emptyTicks.remove(uuid);
        lastBuffs.remove(uuid);
        dismissHud(playerRef, store, ref);
    }

    public boolean isEnabled(UUID uuid) {
        return Boolean.TRUE.equals(tracking.get(uuid));
    }

    public boolean isVisible(UUID uuid) {
        return active.containsKey(uuid);
    }

    public List<ActiveBuff> readActive(PlayerRef playerRef, Store<EntityStore> store, Ref<EntityStore> ref) {
        return snapshots.snapshot(playerRef, store, ref);
    }

    private void startTracking(PlayerRef playerRef, Store<EntityStore> store, Ref<EntityStore> ref) {
        UUID uuid = playerRef.getUuid();
        tracking.put(uuid, true);
        emptyTicks.put(uuid, 0);
        syncHud(playerRef, store, ref);
    }

    private void syncHud(PlayerRef playerRef, Store<EntityStore> store, Ref<EntityStore> ref) {
        if (!isEnabled(playerRef.getUuid())) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        List<ActiveBuff> buffs = snapshots.snapshot(playerRef, store, ref);
        if (!buffs.isEmpty()) {
            emptyTicks.put(uuid, 0);
            lastBuffs.put(uuid, buffs);
            presentHud(playerRef, store, ref, buffs);
            return;
        }

        List<ActiveBuff> cached = lastBuffs.get(uuid);
        int dismissLimit = dismissLimitFor(cached);
        int ticks = emptyTicks.merge(uuid, 1, Integer::sum);
        if (ticks < dismissLimit) {
            if (cached != null && !cached.isEmpty()) {
                presentHud(playerRef, store, ref, cached);
            }
            return;
        }

        emptyTicks.remove(uuid);
        lastBuffs.remove(uuid);
        dismissHud(playerRef, store, ref);
    }

    private static int dismissLimitFor(List<ActiveBuff> cached) {
        if (cached == null || cached.isEmpty()) {
            return DISMISS_AFTER_EMPTY_TICKS;
        }
        if (cached.stream().anyMatch(BuffLedgerHudService::isMorphEffect)) {
            return DISMISS_AFTER_MORPH_CLEAR_TICKS;
        }
        if (cached.size() >= 2) {
            return DISMISS_AFTER_MULTI_CLEAR_TICKS;
        }
        return DISMISS_AFTER_EMPTY_TICKS;
    }

    private static boolean isMorphEffect(ActiveBuff buff) {
        return buff != null && buff.effectId().startsWith("Potion_Morph_");
    }

    private void refreshAll() {
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            if (playerRef == null || !playerRef.isValid() || !isEnabled(playerRef.getUuid())) {
                continue;
            }
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) {
                continue;
            }
            Store<EntityStore> store = ref.getStore();
            if (store == null) {
                continue;
            }
            EntityStore entityStore = store.getExternalData();
            World world = entityStore != null ? entityStore.getWorld() : null;
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
        syncHud(playerRef, ref.getStore(), ref);
    }

    private void presentHud(
            PlayerRef playerRef,
            Store<EntityStore> store,
            Ref<EntityStore> ref,
            List<ActiveBuff> buffs
    ) {
        boolean wasVisible = active.containsKey(playerRef.getUuid());
        BuffLedgerHud hud = active.computeIfAbsent(playerRef.getUuid(), id -> new BuffLedgerHud(playerRef));
        hud.setLines(formatLines(buffs));

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        if (wasVisible) {
            hud.pushUpdate();
        } else {
            HudBridge.show(player, playerRef, hud);
        }
    }

    private void dismissHud(PlayerRef playerRef, Store<EntityStore> store, Ref<EntityStore> ref) {
        if (!active.containsKey(playerRef.getUuid())) {
            return;
        }
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

    static String[] formatLines(List<ActiveBuff> buffs) {
        String[] lines = new String[BuffLedgerHud.LINE_COUNT];
        Arrays.fill(lines, "");

        if (buffs.isEmpty()) {
            return lines;
        }

        lines[0] = "Effects (" + buffs.size() + ")";
        if (buffs.size() <= MAX_VISIBLE_EFFECTS) {
            for (int i = 0; i < buffs.size(); i++) {
                lines[i + 1] = buffs.get(i).hudToken();
            }
            return lines;
        }

        int showCount = MAX_VISIBLE_EFFECTS - 1;
        for (int i = 0; i < showCount; i++) {
            lines[i + 1] = buffs.get(i).hudToken();
        }
        lines[showCount + 1] = "+" + (buffs.size() - showCount) + " more";
        return lines;
    }
}
