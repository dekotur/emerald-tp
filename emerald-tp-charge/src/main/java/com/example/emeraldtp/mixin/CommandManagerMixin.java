package com.example.emeraldtp.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandManager.class)
public abstract class CommandManagerMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private void emeraldtp$blockIfNoEmerald(ServerCommandSource source, String command, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
        // Normalize leading slash if present
        String raw = command.startsWith("/") ? command.substring(1) : command;

        // Match "tp" command exactly or as prefix followed by space
        if (!(raw.equals("tp") || raw.startsWith("tp "))) {
            return;
        }

        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return; // Non-player sources are not charged/blocked
        }

        // Check for at least one emerald in player's inventory
        int emeraldSlot = findEmeraldSlot(player);
        if (emeraldSlot == -1) {
            player.sendMessage(new LiteralText("You need 1 Emerald to use /tp.").styled(s -> s.withColor(0xE53935)), false);
            cir.setReturnValue(0);
            cir.cancel();
            return;
        }
    }

    @Inject(method = "execute", at = @At("RETURN"))
    private void emeraldtp$chargeOnSuccess(ServerCommandSource source, String command, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
        // Only charge if command succeeded (affected > 0) and it's a tp command by a player
        String raw = command.startsWith("/") ? command.substring(1) : command;
        if (!(raw.equals("tp") || raw.startsWith("tp "))) {
            return;
        }
        if (cir.getReturnValue() == null || cir.getReturnValue() <= 0) {
            return;
        }
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return;
        }
        int emeraldSlot = findEmeraldSlot(player);
        if (emeraldSlot == -1) {
            // Should not happen because we checked at HEAD, but guard anyway
            return;
        }
        ItemStack stack = player.getInventory().getStack(emeraldSlot);
        stack.decrement(1);
        if (stack.isEmpty()) {
            player.getInventory().setStack(emeraldSlot, ItemStack.EMPTY);
        }
        player.currentScreenHandler.sendContentUpdates();
    }

    private static int findEmeraldSlot(ServerPlayerEntity player) {
        // Search main inventory then hotbar for any emerald
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (!s.isEmpty() && s.isOf(Items.EMERALD)) {
                return i;
            }
        }
        return -1;
    }
}


