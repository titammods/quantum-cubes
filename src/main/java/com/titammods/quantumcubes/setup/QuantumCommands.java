package com.titammods.quantumcubes.setup;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.titammods.quantumcubes.QuantumCubes;
import com.titammods.quantumcubes.dimension.CubeGeometry;
import com.titammods.quantumcubes.dimension.CubeLayout;
import com.titammods.quantumcubes.dimension.CubeRecord;
import com.titammods.quantumcubes.dimension.QuantumDimension;
import com.titammods.quantumcubes.dimension.QuantumSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = QuantumCubes.MODID)
public class QuantumCommands {

    private QuantumCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("quantumcubes")
                .then(Commands.literal("eject")
                        .executes(ctx -> eject(ctx, List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("players", EntityArgument.players())
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(ctx -> eject(ctx, EntityArgument.getPlayers(ctx, "players")))))
                .then(Commands.literal("list")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(QuantumCommands::list))
                .then(Commands.literal("tp")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                .executes(QuantumCommands::teleport)))
                .then(Commands.literal("spawn")
                        .then(Commands.literal("reset")
                                .executes(QuantumCommands::resetSpawn))
                        .then(Commands.literal("default")
                                .executes(QuantumCommands::setDefaultSpawn)
                                .then(Commands.literal("reset")
                                        .executes(QuantumCommands::resetDefaultSpawn)))));
    }

    private static int eject(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer player : players) {
            if (QuantumDimension.eject(player)) {
                count++;
            }
        }
        if (count == 0) {
            ctx.getSource().sendFailure(Component.translatable("command.quantumcubes.eject.none"));
            return 0;
        }
        int ejected = count;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.quantumcubes.eject.done", ejected), true);
        return count;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        ServerLevel dimension = QuantumDimension.level(server);
        if (dimension == null) {
            ctx.getSource().sendFailure(Component.translatable("gui.quantumcubes.dim_missing"));
            return 0;
        }
        QuantumSavedData data = QuantumSavedData.get(dimension);
        Collection<CubeRecord> cubes = data.cubes();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.quantumcubes.list.header",
                cubes.size(), data.nextIndex()).withStyle(ChatFormatting.AQUA), false);
        for (CubeRecord record : cubes) {
            CubeGeometry geometry = record.geometry();
            int s = geometry.interior();
            Component layout = record.layout() == CubeLayout.LEGACY
                    ? Component.translatable("command.quantumcubes.list.legacy")
                    : Component.translatable("command.quantumcubes.list.chunk");
            Component owner = record.owner()
                    .map(uuid -> ownerName(server, uuid, record))
                    .orElse(Component.literal("-"));
            ctx.getSource().sendSuccess(() -> Component.translatable("command.quantumcubes.list.entry",
                    record.index(), record.tier().ordinal() + 1, s + "×" + s + "×" + s, layout, owner,
                    geometry.minX(), geometry.minY(), geometry.minZ()), false);
        }
        return cubes.size();
    }

    private static int teleport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int index = IntegerArgumentType.getInteger(ctx, "index");
        if (!QuantumDimension.enterByIndex(player, index)) {
            ctx.getSource().sendFailure(Component.translatable("command.quantumcubes.tp.unknown", index));
            return 0;
        }
        return 1;
    }

    private static int resetSpawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!QuantumDimension.isQuantum(player) || !QuantumDimension.resetSpawn(player)) {
            ctx.getSource().sendFailure(Component.translatable("command.quantumcubes.spawn.none"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.quantumcubes.spawn.reset"), false);
        return 1;
    }

    private static int setDefaultSpawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!QuantumDimension.isQuantum(player)) {
            ctx.getSource().sendFailure(Component.translatable("command.quantumcubes.spawn.default_outside"));
            return 0;
        }
        return reportOwner(ctx, QuantumDimension.setDefaultSpawn(player, isOperator(ctx)),
                "command.quantumcubes.spawn.default_set");
    }

    private static int resetDefaultSpawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!QuantumDimension.isQuantum(player)) {
            ctx.getSource().sendFailure(Component.translatable("command.quantumcubes.spawn.default_outside"));
            return 0;
        }
        return reportOwner(ctx, QuantumDimension.resetDefaultSpawn(player, isOperator(ctx)),
                "command.quantumcubes.spawn.default_reset");
    }

    private static int reportOwner(CommandContext<CommandSourceStack> ctx, QuantumDimension.OwnerResult result,
                                   String successKey) {
        String failure = switch (result) {
            case DONE -> null;
            case NONE -> "command.quantumcubes.spawn.default_none";
            case NOT_OWNER -> "command.quantumcubes.spawn.not_owner";
            case OUTSIDE -> "command.quantumcubes.spawn.default_outside";
            case UNKNOWN -> "message.quantumcubes.spawn_unknown";
        };
        if (failure != null) {
            ctx.getSource().sendFailure(Component.translatable(failure));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(successKey), false);
        return 1;
    }

    private static boolean isOperator(CommandContext<CommandSourceStack> ctx) {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(ctx.getSource());
    }

    private static Component ownerName(MinecraftServer server, UUID uuid, CubeRecord record) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        return Component.literal(record.ownerName().orElse(uuid.toString().substring(0, 8)));
    }
}