package com.titammods.quantumcubes.compat.jade;

import com.titammods.quantumcubes.QuantumCubes;
import com.titammods.quantumcubes.block.entity.QuantumCubeBlockEntity;
import com.titammods.quantumcubes.dimension.CubeBuilder;
import com.titammods.quantumcubes.dimension.CubeRecord;
import com.titammods.quantumcubes.dimension.QuantumDimension;
import com.titammods.quantumcubes.dimension.QuantumSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import java.util.Optional;

public class CubeJadeProvider implements IServerDataProvider<BlockAccessor> {

    public static final CubeJadeProvider INSTANCE = new CubeJadeProvider();

    static final Identifier UID = QuantumCubes.id("cube");

    static final String INDEX = "index";

    static final String INTERIOR = "interior";

    static final String OWNER = "owner";

    static final String DEFAULT_SPAWN = "default_spawn";

    static final String PROGRESS = "progress";

    private CubeJadeProvider() {
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof QuantumCubeBlockEntity be) || !be.isGenerated()) {
            return;
        }
        if (!(accessor.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ServerLevel dimension = QuantumDimension.level(level.getServer());
        if (dimension == null) {
            return;
        }
        int index = be.getCubeIndex();
        Optional<CubeRecord> record = QuantumSavedData.get(dimension).cube(index);
        data.putInt(INDEX, index);
        data.putInt(INTERIOR, record.map(r -> r.geometry().interior()).orElse(be.getTier().legacyInterior()));
        record.flatMap(r -> ownerName(level, r)).ifPresent(name -> data.putString(OWNER, name));
        if (record.isPresent() && record.get().defaultSpawn().isPresent()) {
            data.putBoolean(DEFAULT_SPAWN, true);
        }
        if (CubeBuilder.isBuilding(index)) {
            data.putInt(PROGRESS, CubeBuilder.progress(index));
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    private static Optional<String> ownerName(ServerLevel level, CubeRecord record) {
        if (record.owner().isEmpty()) {
            return Optional.empty();
        }
        ServerPlayer online = level.getServer().getPlayerList().getPlayer(record.owner().get());
        if (online != null) {
            return Optional.of(online.getName().getString());
        }
        return record.ownerName();
    }
}