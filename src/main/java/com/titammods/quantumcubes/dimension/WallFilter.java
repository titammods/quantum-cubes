package com.titammods.quantumcubes.dimension;

import com.titammods.quantumcubes.block.CubeTier;

public class WallFilter {

    private static final boolean[] SHELL = build();

    private WallFilter() {
    }

    private static boolean[] build() {
        int max = 0;
        for (CubeTier tier : CubeTier.values()) {
            max = Math.max(max, Math.max(tier.legacyInterior() + 1, tier.chunks() * 16 - 1));
        }
        boolean[] table = new boolean[max + 1];
        table[0] = true;
        for (CubeTier tier : CubeTier.values()) {
            table[tier.legacyInterior() + 1] = true;
            table[tier.chunks() * 16 - 1] = true;
        }
        return table;
    }

    public static boolean mayBeWall(int x, int y, int z) {
        int length = SHELL.length;
        int localX = Math.floorMod(x, QuantumDimension.SPACING);
        int localY = y - QuantumDimension.FLOOR_Y;
        if (localX >= length || localY < 0 || localY >= length || z < 0 || z >= length) {
            return false;
        }
        return SHELL[localX] || SHELL[localY] || SHELL[z];
    }
}