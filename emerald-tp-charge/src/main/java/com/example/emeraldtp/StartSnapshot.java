package com.example.emeraldtp;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public final class StartSnapshot {
    public final RegistryKey<World> worldKey;
    public final Vec3d position;
    public final float yaw;
    public final float pitch;

    public StartSnapshot(ServerPlayerEntity player) {
        this.worldKey = player.getWorld().getRegistryKey();
        this.position = player.getPos();
        this.yaw = player.getYaw(1.0f);
        this.pitch = player.getPitch(1.0f);
    }
}


