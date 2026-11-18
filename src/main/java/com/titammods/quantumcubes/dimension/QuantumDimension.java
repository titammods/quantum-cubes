package com.titammods.quantumcubes.dimension;

import com.titammods.quantumcubes.block.CubeTier;
import com.titammods.quantumcubes.block.entity.QuantumCubeBlockEntity;
import com.titammods.quantumcubes.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class QuantumDimension {

    public static final int SPACING = 2000;

    public static final int FLOOR_Y = 64;

    public static final int MAX_DEPTH = 16;

    public static final int COOLDOWN_TICKS = 25;

    private static final int WALL_SCAN_RANGE = 160;

    public enum SpawnResult {
        SET,
        OUTSIDE,
        UNKNOWN
    }

    private QuantumDimension() {
    }

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(ModDimensions.QUANTUM_LEVEL);
    }

    public static boolean isQuantum(ServerPlayer player) {
        return player.level().dimension().equals(ModDimensions.QUANTUM_LEVEL);
    }

    public static boolean onCooldown(ServerPlayer player, ItemStack stack) {
        return !stack.isEmpty() && player.getCooldowns().isOnCooldown(stack);
    }

    public static void startCooldown(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty()) {
            player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        }
    }

    public static boolean enter(ServerPlayer player, QuantumCubeBlockEntity be) {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        ServerLevel dimension = level(server);
        if (dimension == null) {
            player.sendSystemMessage(Component.translatable("gui.quantumcubes.dim_missing"));
            return false;
        }
        QuantumSavedData data = QuantumSavedData.get(dimension);
        if (data.returnDepth(player.getUUID()) >= MAX_DEPTH) {
            player.sendOverlayMessage(Component.translatable("message.quantumcubes.too_deep")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            return false;
        }

        CubeTier tier = be.getTier();
        CubeRecord record;
        if (!be.isGenerated()) {
            int index = data.allocate();
            record = new CubeRecord(index, tier, CubeLayout.CHUNK, Optional.of(player.getUUID()), Map.of());
            data.registerCube(record);
            be.setCube(index);
            generateCube(dimension, record.geometry());
        } else {
            record = data.cubeOrLegacy(be.getCubeIndex(), tier);
        }

        data.pushReturn(player.getUUID(), ReturnPoint.of(player));
        teleportInto(player, dimension, record);
        return true;
    }

    public static boolean enterByIndex(ServerPlayer player, int index) {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        ServerLevel dimension = level(server);
        if (dimension == null) {
            return false;
        }
        QuantumSavedData data = QuantumSavedData.get(dimension);
        Optional<CubeRecord> record = data.cube(index);
        if (record.isEmpty() || data.returnDepth(player.getUUID()) >= MAX_DEPTH) {
            return false;
        }
        data.pushReturn(player.getUUID(), ReturnPoint.of(player));
        teleportInto(player, dimension, record.get());
        return true;
    }

    public static void exit(ServerPlayer player) {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        ServerLevel dimension = level(server);
        if (dimension == null) {
            return;
        }
        ReturnPoint ret = QuantumSavedData.get(dimension).popReturn(player.getUUID());
        if (!teleportToPoint(player, server, ret)) {
            teleportToWorldSpawn(player, server);
        }
    }

    public static boolean eject(ServerPlayer player) {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        ServerLevel dimension = level(server);
        if (dimension == null) {
            return false;
        }
        QuantumSavedData data = QuantumSavedData.get(dimension);
        ReturnPoint entry = data.entryPoint(player.getUUID());
        if (entry == null && !isQuantum(player)) {
            return false;
        }
        data.clearReturns(player.getUUID());
        if (!teleportToPoint(player, server, entry)) {
            teleportToWorldSpawn(player, server);
        }
        return true;
    }

    public static void rescue(ServerPlayer player) {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        ServerLevel dimension = level(server);
        if (dimension == null) {
            return;
        }
        player.resetFallDistance();
        QuantumSavedData data = QuantumSavedData.get(dimension);
        Optional<CubeRecord> record = data.cube(indexAt(player.getX()));
        if (record.isPresent()) {
            teleportInto(player, dimension, record.get());
        } else if (data.returnDepth(player.getUUID()) > 0) {
            exit(player);
        } else {
            teleportToWorldSpawn(player, server);
        }
        player.sendOverlayMessage(Component.translatable("message.quantumcubes.rescued")
                .withStyle(ChatFormatting.AQUA));
    }

    public static void clearReturns(ServerPlayer player) {
        ServerLevel dimension = level(((ServerLevel) player.level()).getServer());
        if (dimension != null) {
            QuantumSavedData.get(dimension).clearReturns(player.getUUID());
        }
    }

    public static Optional<CubeRecord> cubeAt(ServerLevel dimension, double x, double y, double z) {
        return QuantumSavedData.get(dimension).cube(indexAt(x))
                .filter(record -> record.geometry().containsInterior(x, y, z));
    }

    public static SpawnResult setSpawn(ServerPlayer player) {
        ServerLevel dimension = (ServerLevel) player.level();
        QuantumSavedData data = QuantumSavedData.get(dimension);
        Optional<CubeRecord> record = data.cube(indexAt(player.getX()));
        if (record.isEmpty()) {
            return SpawnResult.UNKNOWN;
        }
        if (!record.get().geometry().containsInterior(player.getX(), player.getY(), player.getZ())) {
            return SpawnResult.OUTSIDE;
        }
        data.setSpawn(record.get(), player.getUUID(), CubeSpawn.of(player));
        return SpawnResult.SET;
    }

    public static boolean resetSpawn(ServerPlayer player) {
        ServerLevel dimension = (ServerLevel) player.level();
        QuantumSavedData data = QuantumSavedData.get(dimension);
        return cubeAt(dimension, player.getX(), player.getY(), player.getZ())
                .map(record -> data.resetSpawn(record, player.getUUID()))
                .orElse(false);
    }

    public static boolean isInsideCube(ServerLevel dimension, Vec3 target) {
        Optional<CubeRecord> record = QuantumSavedData.get(dimension).cube(indexAt(target.x));
        if (record.isPresent()) {
            return record.get().geometry().containsInterior(target.x, target.y, target.z);
        }
        BlockPos pos = BlockPos.containing(target);
        if (pos.getY() <= FLOOR_Y) {
            return false;
        }
        return hasWall(dimension, pos, -1) && hasWall(dimension, pos, 1);
    }

    public static int interiorOf(MinecraftServer server, int index, CubeTier tier) {
        ServerLevel dimension = level(server);
        if (dimension == null) {
            return tier.legacyInterior();
        }
        return QuantumSavedData.get(dimension).cube(index)
                .map(record -> record.geometry().interior())
                .orElse(tier.legacyInterior());
    }

    public static void generateCube(ServerLevel level, CubeGeometry geometry) {
        int x0 = geometry.minX();
        int y0 = geometry.minY();
        int z0 = geometry.minZ();
        int x1 = geometry.maxX();
        int y1 = geometry.maxY();
        int z1 = geometry.maxZ();

        BlockState wall = ModBlocks.QUANTUM_BLOCK.get().defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    boolean boundary = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
                    if (boundary) {
                        cursor.set(x, y, z);
                        level.setBlock(cursor, wall, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static int indexAt(double x) {
        return Math.floorDiv(Mth.floor(x), SPACING);
    }

    private static boolean hasWall(ServerLevel level, BlockPos start, int step) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        for (int i = 0; i < WALL_SCAN_RANGE; i++) {
            cursor.move(0, step, 0);
            if (cursor.getY() < FLOOR_Y || level.isOutsideBuildHeight(cursor)) {
                return false;
            }
            BlockState state = level.getBlockState(cursor);
            if (state.is(ModBlocks.QUANTUM_BLOCK.get()) || state.is(Blocks.BEDROCK)) {
                return true;
            }
        }
        return false;
    }

    private static void teleportInto(ServerPlayer player, ServerLevel dimension, CubeRecord record) {
        Optional<CubeSpawn> spawn = record.spawnFor(player.getUUID());
        if (spawn.isPresent()) {
            teleport(player, dimension, spawn.get().pos(), spawn.get().yaw(), spawn.get().pitch());
        } else {
            teleport(player, dimension, record.geometry().defaultSpawn(), player.getYRot(), player.getXRot());
        }
    }

    private static boolean teleportToPoint(ServerPlayer player, MinecraftServer server, @Nullable ReturnPoint point) {
        if (point == null) {
            return false;
        }
        ServerLevel target = server.getLevel(point.dimension());
        if (target == null) {
            return false;
        }
        teleport(player, target, point.pos(), point.yaw(), point.pitch());
        return true;
    }

    private static void teleportToWorldSpawn(ServerPlayer player, MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        teleport(player, overworld, Vec3.atBottomCenterOf(overworld.getRespawnData().pos()), 0.0f, 0.0f);
    }

    private static void teleport(ServerPlayer player, ServerLevel level, Vec3 pos, float yaw, float pitch) {
        player.teleport(new TeleportTransition(
                level, pos, Vec3.ZERO, yaw, pitch, TeleportTransition.DO_NOTHING));
    }
}