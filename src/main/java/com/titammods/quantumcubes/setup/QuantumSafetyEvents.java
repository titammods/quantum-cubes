package com.titammods.quantumcubes.setup;

import com.titammods.quantumcubes.QuantumCubes;
import com.titammods.quantumcubes.dimension.ModDimensions;
import com.titammods.quantumcubes.dimension.QuantumDimension;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = QuantumCubes.MODID)
public final class QuantumSafetyEvents {

    private static final int RESCUE_DEPTH = 4;

    private QuantumSafetyEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        if (player.isSpectator() || !QuantumDimension.isQuantum(player)) {
            return;
        }
        if (player.getY() < QuantumDimension.FLOOR_Y - RESCUE_DEPTH) {
            QuantumDimension.rescue(player);
        }
    }

    @SubscribeEvent
    public static void onTeleport(EntityTeleportEvent event) {
        if (!(event instanceof EntityTeleportEvent.EnderPearl
                || event instanceof EntityTeleportEvent.ItemConsumption
                || event instanceof EntityTeleportEvent.EnderEntity)) {
            return;
        }
        ServerLevel target = event.getTargetLevel();
        if (target == null || !target.dimension().equals(ModDimensions.QUANTUM_LEVEL)) {
            return;
        }
        if (QuantumDimension.isInsideCube(target, event.getTarget())) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            player.sendOverlayMessage(Component.translatable("message.quantumcubes.teleport_blocked")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && !event.getTo().equals(ModDimensions.QUANTUM_LEVEL)) {
            QuantumDimension.clearReturns(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !QuantumDimension.isQuantum(player)) {
            QuantumDimension.clearReturns(player);
        }
    }
}