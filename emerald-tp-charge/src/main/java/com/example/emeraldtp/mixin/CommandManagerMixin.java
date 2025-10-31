package com.example.emeraldtp.mixin;

import com.example.emeraldtp.StartSnapshot;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(CommandManager.class)
public abstract class CommandManagerMixin {

    private static final Map<UUID, StartSnapshot> emeraldtp$startByPlayer = new HashMap<>();

    @Inject(method = "execute", at = @At("HEAD"), cancellable = false)
    private void emeraldtp$captureStart(ServerCommandSource source, String command, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
        String raw = command.startsWith("/") ? command.substring(1) : command;
        if (!(raw.equals("tp") || raw.startsWith("tp "))) {
            return;
        }
        ServerPlayerEntity player = source.getEntity() instanceof ServerPlayerEntity ? (ServerPlayerEntity) source.getEntity() : null;
        if (player == null) {
            return;
        }
        emeraldtp$startByPlayer.put(player.getUuid(), new StartSnapshot(player));
    }

    @Inject(method = "execute", at = @At("RETURN"))
    private void emeraldtp$chargeByDistance(ServerCommandSource source, String command, CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
        String raw = command.startsWith("/") ? command.substring(1) : command;
        if (!(raw.equals("tp") || raw.startsWith("tp "))) {
            return;
        }
        if (cir.getReturnValue() == null || cir.getReturnValue() <= 0) {
            return;
        }
        ServerPlayerEntity player = source.getEntity() instanceof ServerPlayerEntity ? (ServerPlayerEntity) source.getEntity() : null;
        if (player == null) {
            return;
        }

        StartSnapshot start = emeraldtp$startByPlayer.remove(player.getUuid());
        if (start == null) {
            return;
        }

        Vec3d endPos = player.getPos();
        double dx = endPos.x - start.position.x;
        double dz = endPos.z - start.position.z;
        double horizontalDistance = MathHelper.sqrt((float)(dx * dx + dz * dz));

        int cost = (int) Math.ceil(horizontalDistance / 1000.0);
        if (cost <= 0) {
            return;
        }

        int available = countEmeralds(player);
        if (available < cost) {
            ServerWorld startWorld = source.getServer().getWorld(start.worldKey);
            if (startWorld != null) {
                player.teleport(startWorld, start.position.x, start.position.y, start.position.z, start.yaw, start.pitch);
            }
            player.sendMessage(new LiteralText("Not enough emeralds: need " + cost + ", you have " + available + ".").styled(s -> s.withColor(0xE53935)), false);
            return;
        }

        removeEmeralds(player, cost);
        player.sendMessage(new LiteralText("Charged: " + cost + " emerald(s) (" + (int) Math.ceil(horizontalDistance) + " blocks)").styled(s -> s.withColor(0x43A047)), false);
        player.currentScreenHandler.sendContentUpdates();
    }

    private static int countEmeralds(ServerPlayerEntity player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(Items.EMERALD)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void removeEmeralds(ServerPlayerEntity player, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(Items.EMERALD)) {
                int remove = Math.min(stack.getCount(), remaining);
                stack.decrement(remove);
                if (stack.isEmpty()) {
                    player.getInventory().setStack(i, ItemStack.EMPTY);
                }
                remaining -= remove;
            }
        }
    }

    // helper moved to com.example.emeraldtp.StartSnapshot to comply with mixin package rules
}


