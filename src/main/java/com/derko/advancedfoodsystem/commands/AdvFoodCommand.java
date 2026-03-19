package com.derko.advancedfoodsystem.commands;

import com.derko.advancedfoodsystem.config.ConfigManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class AdvFoodCommand {
    private AdvFoodCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("advfood")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    ConfigManager.loadOrCreate();
                                    context.getSource().sendSuccess(() -> Component.literal("Advanced Food config reloaded."), true);
                                    return 1;
                                }))
                        .then(Commands.literal("debughunger")
                                .then(Commands.literal("on")
                                        .executes(context -> {
                                            ConfigManager.setHungerDebugEnabled(true);
                                            context.getSource().sendSuccess(() -> Component.literal("Advanced Food hunger debug enabled."), true);
                                            return 1;
                                        }))
                                .then(Commands.literal("off")
                                        .executes(context -> {
                                            ConfigManager.setHungerDebugEnabled(false);
                                            context.getSource().sendSuccess(() -> Component.literal("Advanced Food hunger debug disabled."), true);
                                            return 1;
                                        })))
        );
    }
}
