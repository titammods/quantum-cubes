package com.titammods.quantumcubes.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.titammods.quantumcubes.block.CubeTier;
import net.minecraft.core.UUIDUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public class CubeRecord {

    public static final Codec<CubeRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("index").forGetter(CubeRecord::index),
            Codec.STRING.fieldOf("tier").forGetter(record -> record.tier.name()),
            Codec.STRING.fieldOf("layout").forGetter(record -> record.layout.id()),
            UUIDUtil.STRING_CODEC.lenientOptionalFieldOf("owner").forGetter(CubeRecord::owner),
            Codec.STRING.lenientOptionalFieldOf("owner_name").forGetter(CubeRecord::ownerName),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, CubeSpawn.CODEC)
                    .lenientOptionalFieldOf("spawns", Map.of())
                    .forGetter(record -> record.spawns),
            CubeSpawn.CODEC.lenientOptionalFieldOf("default_spawn")
                    .forGetter(record -> Optional.ofNullable(record.defaultSpawn))
    ).apply(instance, (index, tier, layout, owner, ownerName, spawns, defaultSpawn) ->
            new CubeRecord(index, CubeTier.byName(tier), CubeLayout.byId(layout), owner,
                    ownerName.orElse(null), spawns, defaultSpawn.orElse(null))));

    private final int index;
    private final CubeTier tier;
    private final CubeLayout layout;
    private final Optional<UUID> owner;
    private final Map<UUID, CubeSpawn> spawns;
    @Nullable
    private String ownerName;
    @Nullable
    private CubeSpawn defaultSpawn;

    public CubeRecord(int index, CubeTier tier, CubeLayout layout, Optional<UUID> owner, Map<UUID, CubeSpawn> spawns) {
        this(index, tier, layout, owner, null, spawns, null);
    }

    public CubeRecord(int index, CubeTier tier, CubeLayout layout, Optional<UUID> owner, @Nullable String ownerName,
                      Map<UUID, CubeSpawn> spawns, @Nullable CubeSpawn defaultSpawn) {
        this.index = index;
        this.tier = tier;
        this.layout = layout;
        this.owner = owner;
        this.ownerName = ownerName;
        this.spawns = new HashMap<>(spawns);
        this.defaultSpawn = defaultSpawn;
    }

    public int index() {
        return index;
    }

    public CubeTier tier() {
        return tier;
    }

    public CubeLayout layout() {
        return layout;
    }

    public Optional<UUID> owner() {
        return owner;
    }

    public Optional<String> ownerName() {
        return Optional.ofNullable(ownerName);
    }

    public boolean isOwner(UUID player) {
        return owner.isPresent() && owner.get().equals(player);
    }

    public CubeGeometry geometry() {
        return CubeGeometry.of(index, tier, layout);
    }

    public Optional<CubeSpawn> spawnFor(UUID player) {
        return valid(spawns.get(player));
    }

    public Optional<CubeSpawn> defaultSpawn() {
        return valid(defaultSpawn);
    }

    public Optional<CubeSpawn> resolveSpawn(UUID player) {
        return spawnFor(player).or(this::defaultSpawn);
    }

    private Optional<CubeSpawn> valid(@Nullable CubeSpawn spawn) {
        if (spawn == null || !geometry().containsInterior(spawn.x(), spawn.y(), spawn.z())) {
            return Optional.empty();
        }
        return Optional.of(spawn);
    }

    void setSpawn(UUID player, CubeSpawn spawn) {
        spawns.put(player, spawn);
    }

    boolean resetSpawn(UUID player) {
        return spawns.remove(player) != null;
    }

    void setDefaultSpawn(CubeSpawn spawn) {
        defaultSpawn = spawn;
    }

    boolean resetDefaultSpawn() {
        boolean had = defaultSpawn != null;
        defaultSpawn = null;
        return had;
    }

    boolean setOwnerName(String name) {
        if (Objects.equals(ownerName, name)) {
            return false;
        }
        ownerName = name;
        return true;
    }
}