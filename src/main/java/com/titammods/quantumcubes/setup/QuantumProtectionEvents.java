package com.titammods.quantumcubes.setup;

import com.titammods.quantumcubes.QuantumCubes;
import com.titammods.quantumcubes.block.QuantumWallBlock;
import com.titammods.quantumcubes.registry.ModBlocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
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
}