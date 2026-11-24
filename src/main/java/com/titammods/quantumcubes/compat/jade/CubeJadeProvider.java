package com.titammods.quantumcubes.compat.jade;

import com.titammods.quantumcubes.QuantumCubes;
import com.titammods.quantumcubes.block.CubeTier;
import com.titammods.quantumcubes.block.QuantumCubeBlock;
import com.titammods.quantumcubes.block.entity.QuantumCubeBlockEntity;
import com.titammods.quantumcubes.dimension.CubeBuilder;
import com.titammods.quantumcubes.dimension.CubeRecord;
import com.titammods.quantumcubes.dimension.QuantumDimension;
import com.titammods.quantumcubes.dimension.QuantumSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Optional;

public class CubeJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    public static final CubeJadeProvider INSTANCE = new CubeJadeProvider();

    private static final Identifier UID = QuantumCubes.id("cube");

    private static final String INDEX = "index";

    private static final String INTERIOR = "interior";

    private static final String OWNER = "owner";

    private static final String DEFAULT_SPAWN = "default_spawn";

    private static final String PROGRESS = "progress";

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
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlock() instanceof QuantumCubeBlock cube)) {
            return;
        }
        CubeTier tier = cube.getTier();
        CompoundTag data = accessor.getServerData();
        if (!data.contains(INDEX)) {
            if (accessor.isServerConnected()) {
                tooltip.add(size(tier.interiorSize(), tier));
                tooltip.add(Component.translatable("jade.quantumcubes.unbound").withStyle(ChatFormatting.DARK_GRAY));
            }
            return;
        }
        tooltip.add(Component.translatable("jade.quantumcubes.index",
                        Component.literal("#" + data.getIntOr(INDEX, 0)).withStyle(tier.accentColor()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(size(data.getIntOr(INTERIOR, tier.legacyInterior()), tier));
        if (data.contains(OWNER)) {
            tooltip.add(Component.translatable("jade.quantumcubes.owner",
                            Component.literal(data.getStringOr(OWNER, "")).withStyle(ChatFormatting.WHITE))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (data.getBooleanOr(DEFAULT_SPAWN, false)) {
            tooltip.add(Component.translatable("jade.quantumcubes.default_spawn").withStyle(ChatFormatting.DARK_AQUA));
        }
        if (data.contains(PROGRESS)) {
            tooltip.add(Component.translatable("message.quantumcubes.building", data.getIntOr(PROGRESS, 0))
                    .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    private static Component size(int s, CubeTier tier) {
        return Component.translatable("tooltip.quantumcubes.interior",
                        Component.literal(s + " × " + s + " × " + s).withStyle(tier.accentColor()))
                .withStyle(ChatFormatting.GRAY);
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