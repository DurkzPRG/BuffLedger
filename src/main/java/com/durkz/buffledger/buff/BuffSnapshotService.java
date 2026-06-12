package com.durkz.buffledger.buff;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BuffSnapshotService {

    public List<ActiveBuff> snapshot(PlayerRef playerRef, Store<EntityStore> store, Ref<EntityStore> ref) {
        EffectControllerComponent controller = resolveController(playerRef, store, ref);
        if (controller == null) {
            return List.of();
        }

        List<ActiveBuff> out = collectFromController(controller);
        if (!out.isEmpty()) {
            return out;
        }

        ActiveEntityEffect[] active = controller.getAllActiveEntityEffects();
        if (active == null || active.length == 0) {
            return List.of();
        }
        out = new ArrayList<>(active.length);
        for (ActiveEntityEffect entry : active) {
            if (entry != null) {
                out.add(toActiveBuff(entry));
            }
        }
        return sortAndFreeze(out);
    }

    private static EffectControllerComponent resolveController(
            PlayerRef playerRef,
            Store<EntityStore> store,
            Ref<EntityStore> ref
    ) {
        if (store != null && ref != null) {
            EffectControllerComponent fromStore = store.getComponent(ref, EffectControllerComponent.getComponentType());
            if (fromStore != null) {
                return fromStore;
            }
        }
        if (playerRef != null && playerRef.getHolder() != null) {
            return playerRef.getHolder().getComponent(EffectControllerComponent.getComponentType());
        }
        return null;
    }

    private static List<ActiveBuff> collectFromController(EffectControllerComponent controller) {
        Int2ObjectMap<ActiveEntityEffect> active = controller.getActiveEffects();
        if (active == null || active.isEmpty()) {
            return List.of();
        }
        List<ActiveBuff> out = new ArrayList<>(active.size());
        for (ActiveEntityEffect entry : active.values()) {
            if (entry != null) {
                out.add(toActiveBuff(entry));
            }
        }
        return sortAndFreeze(out);
    }

    private static List<ActiveBuff> sortAndFreeze(List<ActiveBuff> out) {
        out.sort(Comparator
                .comparing(ActiveBuff::debuff).reversed()
                .thenComparing(buff -> buff.infinite() ? Long.MAX_VALUE : buff.remainingMs()));
        return List.copyOf(out);
    }

    private static ActiveBuff toActiveBuff(ActiveEntityEffect entry) {
        int index = entry.getEntityEffectIndex();
        EntityEffect def = EntityEffect.getAssetMap().getAsset(index);
        String id = def != null ? def.getId() : Integer.toString(index);
        String name = BuffLabelFormatter.fromEffectId(id);
        boolean infinite = entry.isInfinite();
        long remainingMs = infinite ? -1L : Math.max(0L, (long) (entry.getRemainingDuration() * 1000.0f));
        return new ActiveBuff(id, name, remainingMs, infinite, entry.isDebuff());
    }
}
