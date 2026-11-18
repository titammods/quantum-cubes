package com.titammods.quantumcubes.dimension;

import com.titammods.quantumcubes.block.CubeTier;
import net.minecraft.world.phys.Vec3;

public record CubeGeometry(int minX, int minY, int minZ, int outer, CubeLayout layout) {

    public static CubeGeometry of(int index, CubeTier tier, CubeLayout layout) {
        int outer = layout == CubeLayout.LEGACY ? tier.legacyInterior() + 2 : tier.chunks() * 16;
        return new CubeGeometry(index * QuantumDimension.SPACING, QuantumDimension.FLOOR_Y, 0, outer, layout);
    }

    public int maxX() {
        return minX + outer - 1;
    }

    public int maxY() {
        return minY + outer - 1;
    }

    public int maxZ() {
        return minZ + outer - 1;
    }

    public int interior() {
        return outer - 2;
    }

    public boolean containsInterior(double x, double y, double z) {
        return x >= minX + 1 && x < maxX()
                && y >= minY + 1 && y < maxY()
                && z >= minZ + 1 && z < maxZ();
    }

    public Vec3 defaultSpawn() {
        if (layout == CubeLayout.LEGACY) {
            int size = interior();
            return new Vec3(minX + size / 2.0 + 0.5, minY + 1, minZ + size / 2.0 + 0.5);
        }
        int half = outer / 2;
        return new Vec3(minX + half + 0.5, minY + 1, minZ + half + 0.5);
    }
}