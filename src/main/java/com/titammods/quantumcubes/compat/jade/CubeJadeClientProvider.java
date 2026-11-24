package com.titammods.quantumcubes.compat.jade;

import com.titammods.quantumcubes.block.CubeTier;
import com.titammods.quantumcubes.block.QuantumCubeBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class CubeJadeClientProvider implements IBlockComponentProvider {

    public static final CubeJadeClientProvider INSTANCE = new CubeJadeClientProvider();

    private CubeJadeClientProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlock() instanceof QuantumCubeBlock cube)) {
            return;
        }
        CubeTier tier = cube.getTier();
        CompoundTag data = accessor.getServerData();
        if (!data.contains(CubeJadeProvider.INDEX)) {
            if (accessor.isServerConnected()) {
                tooltip.add(size(tier.interiorSize(), tier));
                tooltip.add(Component.translatable("jade.quantumcubes.unbound").withStyle(ChatFormatting.DARK_GRAY));
            }
            return;
        }
        tooltip.add(Component.translatable("jade.quantumcubes.index",
                        Component.literal("#" + data.getIntOr(CubeJadeProvider.INDEX, 0)).withStyle(tier.accentColor()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(size(data.getIntOr(CubeJadeProvider.INTERIOR, tier.legacyInterior()), tier));
        if (data.contains(CubeJadeProvider.OWNER)) {
            tooltip.add(Component.translatable("jade.quantumcubes.owner",
                            Component.literal(data.getStringOr(CubeJadeProvider.OWNER, "")).withStyle(ChatFormatting.WHITE))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (data.getBooleanOr(CubeJadeProvider.DEFAULT_SPAWN, false)) {
            tooltip.add(Component.translatable("jade.quantumcubes.default_spawn").withStyle(ChatFormatting.DARK_AQUA));
        }
        if (data.contains(CubeJadeProvider.PROGRESS)) {
            tooltip.add(Component.translatable("message.quantumcubes.building", data.getIntOr(CubeJadeProvider.PROGRESS, 0))
                    .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        }
    }

    @Override
    public Identifier getUid() {
        return CubeJadeProvider.UID;
    }

    private static Component size(int s, CubeTier tier) {
        return Component.translatable("tooltip.quantumcubes.interior",
                        Component.literal(s + " × " + s + " × " + s).withStyle(tier.accentColor()))
                .withStyle(ChatFormatting.GRAY);
    }
}