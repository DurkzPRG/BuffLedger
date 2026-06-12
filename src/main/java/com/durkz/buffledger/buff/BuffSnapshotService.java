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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BuffSnapshotService {

    public List<ActiveBuff> snapshot(PlayerRef playerRef, Store<EntityStore> store, Ref<EntityStore> ref) {
        EffectControllerComponent controller = resolveController(playerRef, store, ref);
        if (controller == null) {
            return List.of();
        }
        return collectAll(controller);
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

    private static List<ActiveBuff> collectAll(EffectControllerComponent controller) {
        Map<Integer, ActiveEntityEffect> unique = new LinkedHashMap<>();

        Int2ObjectMap<ActiveEntityEffect> active = controller.getActiveEffects();
        if (active != null) {
            for (ActiveEntityEffect entry : active.values()) {
                if (entry != null) {
                    unique.put(entry.getEntityEffectIndex(), entry);
                }
            }
        }

        ActiveEntityEffect[] all = controller.getAllActiveEntityEffects();
        if (all != null) {
            for (ActiveEntityEffect entry : all) {
                if (entry != null) {
                    unique.putIfAbsent(entry.getEntityEffectIndex(), entry);
                }
            }
        }

        if (unique.isEmpty()) {
            return List.of();
        }

        List<ActiveBuff> out = new ArrayList<>(unique.size());
        for (ActiveEntityEffect entry : unique.values()) {
            out.add(toActiveBuff(entry));
        }
        return mergeStacks(out);
    }

    private static List<ActiveBuff> mergeStacks(List<ActiveBuff> raw) {
        if (raw.size() <= 1) {
            return sortAndFreeze(raw);
        }

        Map<String, ActiveBuff> merged = new LinkedHashMap<>();
        Map<String, Integer> stackCounts = new LinkedHashMap<>();
        for (ActiveBuff buff : raw) {
            stackCounts.merge(buff.effectId(), 1, Integer::sum);
            ActiveBuff existing = merged.get(buff.effectId());
            if (existing == null) {
                merged.put(buff.effectId(), buff);
                continue;
            }
            merged.put(buff.effectId(), new ActiveBuff(
                    buff.effectId(),
                    buff.displayName(),
                    Math.max(existing.remainingMs(), buff.remainingMs()),
                    existing.infinite() || buff.infinite(),
                    existing.debuff(),
                    existing.stacks()
            ));
        }

        List<ActiveBuff> out = new ArrayList<>(merged.size());
        for (Map.Entry<String, ActiveBuff> entry : merged.entrySet()) {
            ActiveBuff buff = entry.getValue();
            int stacks = stackCounts.getOrDefault(entry.getKey(), 1);
            if (stacks <= 1) {
                out.add(buff);
            } else {
                out.add(new ActiveBuff(
                        buff.effectId(),
                        buff.displayName(),
                        buff.remainingMs(),
                        buff.infinite(),
                        buff.debuff(),
                        stacks
                ));
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
