package com.titammods.quantumcubes.block;

import net.minecraft.ChatFormatting;

public enum CubeTier {
    TIER_1("quantum_cube_tier_1", 20, 1, ChatFormatting.GRAY),
    TIER_2("quantum_cube_tier_2", 40, 3, ChatFormatting.WHITE),
    TIER_3("quantum_cube_tier_3", 64, 5, ChatFormatting.GOLD),
    TIER_4("quantum_cube_tier_4", 96, 7, ChatFormatting.AQUA),
    TIER_5("quantum_cube_tier_5", 128, 9, ChatFormatting.GREEN);

    private final String blockName;
    private final int legacyInterior;
    private final int chunks;
    private final ChatFormatting accentColor;

    CubeTier(String blockName, int legacyInterior, int chunks, ChatFormatting accentColor) {
        this.blockName = blockName;
        this.legacyInterior = legacyInterior;
        this.chunks = chunks;
        this.accentColor = accentColor;
    }

    public String blockName() {
        return blockName;
    }

    public int chunks() {
        return chunks;
    }

    public int interiorSize() {
        return chunks * 16 - 2;
    }

    public int legacyInterior() {
        return legacyInterior;
    }

    public ChatFormatting accentColor() {
        return accentColor;
    }

    public static CubeTier byName(String name) {
        for (CubeTier tier : values()) {
            if (tier.name().equals(name)) {
                return tier;
            }
        }
        return TIER_1;
    }
}