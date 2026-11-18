package com.titammods.quantumcubes.setup;

import com.titammods.quantumcubes.QuantumCubes;
import com.titammods.quantumcubes.block.QuantumCubeBlock;
import com.titammods.quantumcubes.dimension.QuantumDimension;
import com.titammods.quantumcubes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = QuantumCubes.MODID)
public final class QuantumEvents {

    private QuantumEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !QuantumDimension.isQuantum(player)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.QUANTUM_REMOTE.get())) {
            return;
        }
        if (!player.isShiftKeyDown()
                && player.level().getBlockState(event.getPos()).getBlock() instanceof QuantumCubeBlock) {
            return;
        }
        event.setCanceled(true);
        if (QuantumDimension.onCooldown(player, stack)) {
            return;
        }
        if (player.isShiftKeyDown()) {
            defineSpawn(player);
        } else {
            QuantumDimension.exit(player);
        }
        QuantumDimension.startCooldown(player, stack);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !QuantumDimension.isQuantum(player)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.QUANTUM_REMOTE.get()) || !player.isShiftKeyDown()) {
            return;
        }
        event.setCanceled(true);
        if (QuantumDimension.onCooldown(player, stack)) {
            return;
        }
        defineSpawn(player);
        QuantumDimension.startCooldown(player, stack);
    }

    private static void defineSpawn(ServerPlayer player) {
        Component message = switch (QuantumDimension.setSpawn(player)) {
            case SET -> Component.translatable("message.quantumcubes.spawn_set").withStyle(ChatFormatting.AQUA);
            case OUTSIDE -> Component.translatable("message.quantumcubes.spawn_outside").withStyle(ChatFormatting.RED);
            case UNKNOWN -> Component.translatable("message.quantumcubes.spawn_unknown").withStyle(ChatFormatting.YELLOW);
        };
        player.sendOverlayMessage(message);
    }
}