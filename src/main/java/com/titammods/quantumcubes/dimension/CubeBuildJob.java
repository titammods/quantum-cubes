package com.titammods.quantumcubes.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class CubeBuildJob {

    record Waiter(UUID player, ResourceKey<Level> dimension, BlockPos origin) {
    }

    private final int index;
    private final CubeGeometry geometry;
    private final int minChunkX;
    private final int maxChunkX;
    private final int minChunkZ;
    private final int maxChunkZ;
    private final long total;
    private final List<Waiter> waiters = new ArrayList<>();
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private int chunkX;
    private int chunkZ;
    private int x;
    private int y;
    private int z;
    private long placed;
    private boolean done;

    CubeBuildJob(int index, CubeGeometry geometry) {
        this.index = index;
        this.geometry = geometry;
        this.minChunkX = SectionPos.blockToSectionCoord(geometry.minX());
        this.maxChunkX = SectionPos.blockToSectionCoord(geometry.maxX());
        this.minChunkZ = SectionPos.blockToSectionCoord(geometry.minZ());
        this.maxChunkZ = SectionPos.blockToSectionCoord(geometry.maxZ());
        long outer = geometry.outer();
        long inner = Math.max(0, outer - 2);
        this.total = outer * outer * outer - inner * inner * inner;
        this.chunkX = minChunkX;
        this.chunkZ = minChunkZ;
        startChunk();
    }

    int index() {
        return index;
    }

    long total() {
        return total;
    }

    boolean done() {
        return done;
    }

    int progress() {
        return total == 0 ? 100 : (int) Math.min(99, placed * 100 / total);
    }

    List<Waiter> waiters() {
        return waiters;
    }

    void addWaiter(Waiter waiter) {
        waiters.removeIf(existing -> existing.player().equals(waiter.player()));
        waiters.add(waiter);
    }

    int run(ServerLevel level, BlockState wall, int budget) {
        int used = 0;
        while (!done && used < budget) {
            cursor.set(x, y, z);
            level.setBlock(cursor, wall, Block.UPDATE_CLIENTS);
            used++;
            placed++;
            advance();
        }
        return used;
    }

    private void advance() {
        boolean side = x == geometry.minX() || x == geometry.maxX()
                || z == geometry.minZ() || z == geometry.maxZ();
        if (side) {
            y++;
        } else {
            y = y == geometry.minY() ? geometry.maxY() : geometry.maxY() + 1;
        }
        if (y <= geometry.maxY()) {
            return;
        }
        y = geometry.minY();
        z++;
        if (z <= columnMaxZ()) {
            return;
        }
        z = columnMinZ();
        x++;
        if (x <= columnMaxX()) {
            return;
        }
        chunkX++;
        if (chunkX > maxChunkX) {
            chunkX = minChunkX;
            chunkZ++;
            if (chunkZ > maxChunkZ) {
                done = true;
                return;
            }
        }
        startChunk();
    }

    private void startChunk() {
        x = columnMinX();
        z = columnMinZ();
        y = geometry.minY();
    }

    private int columnMinX() {
        return Math.max(geometry.minX(), SectionPos.sectionToBlockCoord(chunkX));
    }

    private int columnMaxX() {
        return Math.min(geometry.maxX(), SectionPos.sectionToBlockCoord(chunkX) + 15);
    }

    private int columnMinZ() {
        return Math.max(geometry.minZ(), SectionPos.sectionToBlockCoord(chunkZ));
    }

    private int columnMaxZ() {
        return Math.min(geometry.maxZ(), SectionPos.sectionToBlockCoord(chunkZ) + 15);
    }
}