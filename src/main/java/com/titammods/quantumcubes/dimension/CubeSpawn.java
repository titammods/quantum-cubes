package com.titammods.quantumcubes.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public record CubeSpawn(double x, double y, double z, float yaw, float pitch) {

    public static final Codec<CubeSpawn> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("x").forGetter(CubeSpawn::x),
            Codec.DOUBLE.fieldOf("y").forGetter(CubeSpawn::y),
            Codec.DOUBLE.fieldOf("z").forGetter(CubeSpawn::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(CubeSpawn::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(CubeSpawn::pitch)
    ).apply(instance, CubeSpawn::new));

    public static CubeSpawn of(ServerPlayer player) {
        return new CubeSpawn(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }

    public Vec3 pos() {
        return new Vec3(x, y, z);
    }
}