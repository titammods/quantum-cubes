package com.titammods.quantumcubes.setup;

import com.titammods.quantumcubes.QuantumCubes;
import com.titammods.quantumcubes.block.QuantumWallBlock;
import com.titammods.quantumcubes.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

@EventBusSubscriber(modid = QuantumCubes.MODID)
public final class QuantumProtectionEvents {

    private QuantumProtectionEvents() {
    }

    @SubscribeEvent
    public static void onBreakBlock(BreakBlockEvent event) {
        if (event.getState().is(ModBlocks.QUANTUM_BLOCK.get()) && !QuantumWallBlock.canBreak(event.getPlayer())) {
            event.setCanceled(true);
            event.setNotifyClient(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDestroyBlock(LivingDestroyBlockEvent event) {
        if (event.getState().is(ModBlocks.QUANTUM_BLOCK.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!player.isCreative() || player.isShiftKeyDown()) {
            return;
        }
        if (!event.getLevel().getBlockState(event.getPos()).is(ModBlocks.QUANTUM_BLOCK.get())) {
            return;
        }
        event.setCanceled(true);
        if (player instanceof ServerPlayer serverPlayer
                && event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.quantumcubes.wall_shift")
                    .withStyle(ChatFormatting.RED));
        }
    }
}