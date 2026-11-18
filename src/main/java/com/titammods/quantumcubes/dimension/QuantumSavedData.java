package com.titammods.quantumcubes.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.quantumcubes.QuantumCubes;
import com.titammods.quantumcubes.block.CubeTier;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public class QuantumSavedData extends SavedData {

    public static final Codec<QuantumSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("next_index").forGetter(data -> data.nextIndex),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, ReturnPoint.CODEC)
                    .optionalFieldOf("returns", Map.of())
                    .forGetter(QuantumSavedData::entryPoints),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, ReturnPoint.CODEC.listOf())
                    .lenientOptionalFieldOf("return_stacks", Map.of())
                    .forGetter(data -> data.returnStacks),
            CubeRecord.CODEC.listOf()
                    .lenientOptionalFieldOf("cubes", List.of())
                    .forGetter(data -> List.copyOf(data.cubes.values())),
            Codec.INT.listOf()
                    .lenientOptionalFieldOf("pending_builds", List.of())
                    .forGetter(data -> List.copyOf(data.pending))
    ).apply(instance, QuantumSavedData::new));

    public static final SavedDataType<QuantumSavedData> TYPE = new SavedDataType<QuantumSavedData>(
            QuantumCubes.id("quantum_data"),
            () -> new QuantumSavedData(),
            CODEC,
            null);

    private int nextIndex;
    private final Map<UUID, List<ReturnPoint>> returnStacks;
    private final Map<Integer, CubeRecord> cubes;
    private final Set<Integer> pending;

    public QuantumSavedData() {
        this(0, Map.of(), Map.of(), List.of(), List.of());
    }

    public QuantumSavedData(int nextIndex, Map<UUID, ReturnPoint> legacyReturns,
                            Map<UUID, List<ReturnPoint>> returnStacks, List<CubeRecord> cubes,
                            List<Integer> pending) {
        this.nextIndex = nextIndex;
        this.returnStacks = new HashMap<>();
        returnStacks.forEach((player, stack) -> {
            if (!stack.isEmpty()) {
                this.returnStacks.put(player, new ArrayList<>(stack));
            }
        });
        legacyReturns.forEach((player, point) -> {
            if (!this.returnStacks.containsKey(player)) {
                List<ReturnPoint> stack = new ArrayList<>();
                stack.add(point);
                this.returnStacks.put(player, stack);
            }
        });
        this.cubes = new TreeMap<>();
        for (CubeRecord record : cubes) {
            this.cubes.put(record.index(), record);
        }
        this.pending = new LinkedHashSet<>(pending);
    }

    private Map<UUID, ReturnPoint> entryPoints() {
        Map<UUID, ReturnPoint> result = new HashMap<>();
        returnStacks.forEach((player, stack) -> {
            if (!stack.isEmpty()) {
                result.put(player, stack.get(0));
            }
        });
        return result;
    }

    public int nextIndex() {
        return nextIndex;
    }

    public int allocate() {
        int result = nextIndex;
        nextIndex++;
        setDirty();
        return result;
    }

    public int returnDepth(UUID player) {
        List<ReturnPoint> stack = returnStacks.get(player);
        return stack == null ? 0 : stack.size();
    }

    public void pushReturn(UUID player, ReturnPoint point) {
        returnStacks.computeIfAbsent(player, key -> new ArrayList<>()).add(point);
        setDirty();
    }

    @Nullable
    public ReturnPoint popReturn(UUID player) {
        List<ReturnPoint> stack = returnStacks.get(player);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ReturnPoint point = stack.remove(stack.size() - 1);
        if (stack.isEmpty()) {
            returnStacks.remove(player);
        }
        setDirty();
        return point;
    }

    @Nullable
    public ReturnPoint entryPoint(UUID player) {
        List<ReturnPoint> stack = returnStacks.get(player);
        return stack == null || stack.isEmpty() ? null : stack.get(0);
    }

    public void clearReturns(UUID player) {
        if (returnStacks.remove(player) != null) {
            setDirty();
        }
    }

    public Optional<CubeRecord> cube(int index) {
        return Optional.ofNullable(cubes.get(index));
    }

    public Collection<CubeRecord> cubes() {
        return cubes.values();
    }

    public void registerCube(CubeRecord record) {
        cubes.put(record.index(), record);
        setDirty();
    }

    public CubeRecord cubeOrLegacy(int index, CubeTier tier) {
        CubeRecord record = cubes.get(index);
        if (record == null) {
            record = new CubeRecord(index, tier, CubeLayout.LEGACY, Optional.empty(), Map.of());
            registerCube(record);
        }
        return record;
    }

    public Set<Integer> pending() {
        return pending;
    }

    public void markPending(int index) {
        if (pending.add(index)) {
            setDirty();
        }
    }

    public void clearPending(int index) {
        if (pending.remove(index)) {
            setDirty();
        }
    }

    public void setSpawn(CubeRecord record, UUID player, CubeSpawn spawn) {
        record.setSpawn(player, spawn);
        setDirty();
    }

    public boolean resetSpawn(CubeRecord record, UUID player) {
        boolean removed = record.resetSpawn(player);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public static QuantumSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}