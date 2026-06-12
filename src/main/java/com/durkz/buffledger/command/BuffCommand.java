package com.durkz.buffledger.command;

import com.durkz.buffledger.BuffLedgerPlugin;
import com.durkz.buffledger.buff.ActiveBuff;
import com.durkz.buffledger.hud.BuffLedgerHudService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BuffCommand extends AbstractAsyncCommand {

    public BuffCommand() {
        super("buffs", "Buff ledger HUD");
        addSubCommand(new OnCmd());
        addSubCommand(new OffCmd());
        addSubCommand(new StatusCmd());
        addSubCommand(new ListCmd());
    }

    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        ctx.sendMessage(Message.raw("Usage: /buffs on | off | status | list").color("#AAAAAA"));
        return CompletableFuture.completedFuture(null);
    }

    private static BuffLedgerPlugin pluginOrNull() {
        return BuffLedgerPlugin.getInstance();
    }

    private static final class OnCmd extends AbstractPlayerCommand {
        OnCmd() {
            super("on", "Show buff ledger HUD");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef playerRef, World world) {
            BuffLedgerPlugin plugin = pluginOrNull();
            if (plugin == null) {
                return;
            }
            if (plugin.hudService().enable(playerRef, store, ref)) {
                ctx.sendMessage(Message.raw("Buff ledger HUD on").color("#55FF55"));
            }
        }
    }

    private static final class OffCmd extends AbstractPlayerCommand {
        OffCmd() {
            super("off", "Hide buff ledger HUD");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef playerRef, World world) {
            BuffLedgerPlugin plugin = pluginOrNull();
            if (plugin == null) {
                return;
            }
            plugin.hudService().disable(playerRef, store, ref);
            ctx.sendMessage(Message.raw("Buff ledger HUD off").color("#AAAAAA"));
        }
    }

    private static final class StatusCmd extends AbstractPlayerCommand {
        StatusCmd() {
            super("status", "HUD toggle state");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef playerRef, World world) {
            BuffLedgerPlugin plugin = pluginOrNull();
            if (plugin == null) {
                return;
            }
            BuffLedgerHudService hud = plugin.hudService();
            boolean on = hud.isEnabled(playerRef.getUuid());
            int count = hud.readActive(playerRef, store, ref).size();
            ctx.sendMessage(Message.raw("Buff ledger HUD: " + (on ? "on" : "off") + " | tracked effects: " + count)
                    .color("#FFAA00"));
        }
    }

    private static final class ListCmd extends AbstractPlayerCommand {
        ListCmd() {
            super("list", "Print active effects in chat");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef playerRef, World world) {
            BuffLedgerPlugin plugin = pluginOrNull();
            if (plugin == null) {
                return;
            }
            List<ActiveBuff> buffs = plugin.hudService().readActive(playerRef, store, ref);
            if (buffs.isEmpty()) {
                ctx.sendMessage(Message.raw("No active effects on EffectController.").color("#AAAAAA"));
                return;
            }
            ctx.sendMessage(Message.raw("Active effects (" + buffs.size() + "):").color("#FFDD88"));
            for (ActiveBuff buff : buffs) {
                ctx.sendMessage(Message.raw("  " + buff.hudToken()).color(buff.debuff() ? "#FF8888" : "#9FD8FF"));
            }
        }
    }
}
