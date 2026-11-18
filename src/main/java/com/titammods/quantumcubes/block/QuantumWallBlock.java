package com.titammods.quantumcubes.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.util.FakePlayer;

public class QuantumWallBlock extends Block {

    private static final ThreadLocal<Boolean> BYPASS = ThreadLocal.withInitial(() -> false);

    public QuantumWallBlock(Properties properties) {
        super(properties);
    }

    public static boolean canBreak(Player player) {
        return player != null && player.isCreative() && player.isShiftKeyDown() && !(player instanceof FakePlayer);
    }

    public static boolean isBypassing() {
        return BYPASS.get();
    }

    public static void runBypassed(Runnable action) {
        BYPASS.set(true);
        try {
            action.run();
        } finally {
            BYPASS.set(false);
        }
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected int getLightDampening(BlockState state) {
        return 0;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public void onBlockExploded(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion) {
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                       ItemStack toolStack, boolean willHarvest, FluidState fluid) {
        if (!canBreak(player)) {
            return false;
        }
        BYPASS.set(true);
        try {
            return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
        } finally {
            BYPASS.set(false);
        }
    }
}