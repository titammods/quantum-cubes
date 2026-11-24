package com.titammods.quantumcubes.dimension;

import com.titammods.quantumcubes.QuantumCubes;
import com.titammods.quantumcubes.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = QuantumCubes.MODID)
public class CubeBuilder {

    public static final int BLOCKS_PER_TICK = 4096;

    private static final double AWAIT_RANGE_SQR = 64.0;

    private static final int PROGRESS_INTERVAL = 10;

    private static final Map<Integer, CubeBuildJob> JOBS = new LinkedHashMap<>();

    private CubeBuilder() {
    }

    public static boolean build(ServerLevel dimension, CubeRecord record) {
        CubeBuildJob job = new CubeBuildJob(record.index(), record.geometry());
        if (JOBS.isEmpty() && job.total() <= BLOCKS_PER_TICK) {
            job.run(dimension, wall(), Integer.MAX_VALUE);
            return true;
        }
        QuantumSavedData.get(dimension).markPending(record.index());
        JOBS.put(record.index(), job);
        return false;
    }

    public static boolean isBuilding(int index) {
        return JOBS.containsKey(index);
    }

    public static int progress(int index) {
        CubeBuildJob job = JOBS.get(index);
        return job == null ? 100 : job.progress();
    }

    public static void await(int index, ServerPlayer player, BlockPos origin) {
        CubeBuildJob job = JOBS.get(index);
        if (job == null) {
            return;
        }
        job.addWaiter(new CubeBuildJob.Waiter(player.getUUID(), player.level().dimension(), origin.immutable()));
        sendProgress(player, job);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (JOBS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel dimension = QuantumDimension.level(server);
        if (dimension == null) {
            JOBS.clear();
            return;
        }
        BlockState wall = wall();
        int budget = BLOCKS_PER_TICK;
        List<CubeBuildJob> finished = new ArrayList<>();
        Iterator<CubeBuildJob> iterator = JOBS.values().iterator();
        while (budget > 0 && iterator.hasNext()) {
            CubeBuildJob job = iterator.next();
            budget -= job.run(dimension, wall, budget);
            if (job.done()) {
                iterator.remove();
                finished.add(job);
            }
        }
        QuantumSavedData data = QuantumSavedData.get(dimension);
        for (CubeBuildJob job : finished) {
            data.clearPending(job.index());
            deliver(server, dimension, data, job);
        }
        if (server.getTickCount() % PROGRESS_INTERVAL == 0) {
            for (CubeBuildJob job : JOBS.values()) {
                for (CubeBuildJob.Waiter waiter : job.waiters()) {
                    ServerPlayer player = server.getPlayerList().getPlayer(waiter.player());
                    if (player != null) {
                        sendProgress(player, job);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        JOBS.clear();
        ServerLevel dimension = QuantumDimension.level(event.getServer());
        if (dimension == null) {
            return;
        }
        QuantumSavedData data = QuantumSavedData.get(dimension);
        for (int index : List.copyOf(data.pending())) {
            Optional<CubeRecord> record = data.cube(index);
            if (record.isPresent()) {
                JOBS.put(index, new CubeBuildJob(index, record.get().geometry()));
            } else {
                data.clearPending(index);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        JOBS.clear();
    }

    private static void deliver(MinecraftServer server, ServerLevel dimension, QuantumSavedData data,
                                CubeBuildJob job) {
        Optional<CubeRecord> record = data.cube(job.index());
        for (CubeBuildJob.Waiter waiter : job.waiters()) {
            ServerPlayer player = server.getPlayerList().getPlayer(waiter.player());
            if (player == null) {
                continue;
            }
            boolean nearby = player.level().dimension().equals(waiter.dimension())
                    && player.position().distanceToSqr(Vec3.atCenterOf(waiter.origin())) <= AWAIT_RANGE_SQR;
            if (record.isPresent() && nearby && !player.isSpectator()) {
                QuantumDimension.arrive(player, dimension, record.get());
            } else {
                player.sendOverlayMessage(Component.translatable("message.quantumcubes.cube_ready")
                        .withStyle(ChatFormatting.AQUA));
            }
        }
    }

    private static void sendProgress(ServerPlayer player, CubeBuildJob job) {
        player.sendOverlayMessage(Component.translatable("message.quantumcubes.building", job.progress())
                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
    }

    private static BlockState wall() {
        return ModBlocks.QUANTUM_BLOCK.get().defaultBlockState();
    }
}