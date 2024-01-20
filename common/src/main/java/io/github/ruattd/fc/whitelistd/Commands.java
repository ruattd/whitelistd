package io.github.ruattd.fc.whitelistd;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.UUID;
import java.util.function.Predicate;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Commands {
    static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ignored1, net.minecraft.commands.Commands.CommandSelection ignored2) {
        var instance = Whitelistd.getInstance();
        var config = instance.getConfig();
        SuggestionProvider<CommandSourceStack> nameSuggest = (context, builder) -> {
            var players = instance.getServer().getPlayerList().getPlayers();
            for (var i : players) {
                var name = i.getName().getString();
                var uuid = i.getStringUUID();
                builder.suggest(name);
                builder.suggest(name + ' ' + uuid);
            }
            return builder.buildFuture();
        };
        var string = StringArgumentType.string();
        Predicate<CommandSourceStack> require = source -> source.hasPermission(config.getPermissionLevel());
        var whitelistd = dispatcher.register(literal("whitelistd")
                .requires(require)
                .then(literal("add")
                        .then(argument("name", string)
                                .suggests(nameSuggest)
                                .executes(context -> whitelistdCommand(1, false, context))
                                .then(argument("uuid", string)
                                        .executes(context -> whitelistdCommand(1, true, context))
                                )
                        )
                )
                .then(literal("remove")
                        .then(argument("name", string)
                                .suggests(nameSuggest)
                                .executes(context -> whitelistdCommand(2, false, context))
                                .then(argument("uuid", string)
                                        .executes(context -> whitelistdCommand(2, true, context))
                                )
                        )
                )
                .then(literal("query")
                        .then(argument("name", string)
                                .suggests(nameSuggest)
                                .executes(context -> whitelistdCommand(3, false, context))
                                .then(argument("uuid", string)
                                        .executes(context -> whitelistdCommand(3, true, context))
                                )
                        )
                )
                .then(literal("on")
                        .executes(context -> {
                            instance.setEnabled(true);
                            var source = context.getSource();
                            source.sendSystemMessage(Component.translatable("wld.status.enable_whitelist"));
                            MessageHelper.sendLogI(Component.translatable("wld.console.enable_whitelist", source.getTextName()).getString());
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(literal("off")
                        .executes(context -> {
                            instance.setEnabled(false);
                            var source = context.getSource();
                            source.sendSystemMessage(Component.translatable("wld.status.disable_whitelist"));
                            MessageHelper.sendLogW(Component.translatable("wld.console.disable_whitelist", source.getTextName()).getString());
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(literal("list")
                        .executes(context -> {
                            var source = context.getSource();
                            new Thread(() -> {
                                var searchList = Whitelistd.getInstance().getSearchList();
                                searchList.getItems().forEach(info -> source.sendSystemMessage(
                                        Component.literal(info.getName() + '{' + info.getUuid() + '}')));
                            }, "Whitelistd Listing Thread").start();
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );
        dispatcher.register(literal("wld").requires(require).redirect(whitelistd));
        if (config.isEnableRecord()) {
            dispatcher.register(literal("record").requires(require)
                    .then(argument("name", string)
                            .executes(context -> {
                                var name = context.getArgument("name", String.class);
                                if (name != null) {
                                    var searchList = instance.getSearchList();
                                    var result = searchList.query(new PlayerInfo(name));
                                    var source = context.getSource();
                                    if (result.exist()) {
                                        source.sendFailure(Component.translatable("wld.status.duplicated_name"));
                                    } else {
                                        var state = searchList.addItem(new PlayerInfo(name + ".record"));
                                        if (state == SearchList.AddItemState.SUCCESSFUL) {
                                            source.sendSystemMessage(Component.empty().append("Successfully recorded"));
                                            MessageHelper.sendLogI(Component.translatable("wld.console.add_record", source.getTextName(), name).getString());
                                        } else {
                                            source.sendFailure(Component.translatable("wld.status.failed", state.toString()));
                                        }
                                    }
                                }
                                return Command.SINGLE_SUCCESS;
                            })
                    )
            );
        }
    }

    private static int whitelistdCommand(int operation, boolean existUuid, CommandContext<CommandSourceStack> context) {
        try {
            var source = context.getSource();
            var name = context.getArgument("name", String.class);
            var uuid = existUuid ? context.getArgument("uuid", String.class) : null;
            try {
                var playerInfo = new PlayerInfo(name, (uuid == null) ? null : UUID.fromString(uuid));
                var searchList = Whitelistd.getInstance().getSearchList();
                var profileName = name + '{' + uuid + '}';
                switch (operation) {
                    case 1 -> { // add
                        var state = searchList.addItem(playerInfo);
                        if (state == SearchList.AddItemState.SUCCESSFUL) {
                            source.sendSystemMessage(Component.translatable("wld.status.add_whitelist_item"));
                            MessageHelper.sendLogI(Component.translatable("wld.console.add_whitelist_item", source.getTextName(), profileName).getString());
                        } else {
                            source.sendFailure(Component.translatable("wld.status.failed", state.toString()));
                        }
                    }
                    case 2 -> { // remove
                        var state = searchList.removeItem(playerInfo);
                        if (state == SearchList.RemoveItemState.SUCCESSFUL) {
                            source.sendSystemMessage(Component.translatable("wld.status.remove_whitelist_item"));
                            MessageHelper.sendLogI(Component.translatable("wld.console.remove_whitelist_item", source.getTextName(), profileName).getString());
                        } else {
                            source.sendFailure(Component.translatable("wld.status.failed", state.toString()));
                        }
                    }
                    case 3 -> { // query
                        var result = searchList.query(playerInfo);
                        Component message;
                        if (result.exist()) {
                            var stored = result.playerStored();
                            var resultName = stored.getName() + '{' + stored.getUuid() + '}';
                            message = Component.translatable("wld.status.found", resultName);
                        } else {
                            message = Component.translatable("wld.status.not_found");
                        }
                        source.sendSystemMessage(Component.empty().append(message));
                    }
                }
            } catch (IllegalArgumentException e) {
                source.sendFailure(Component.translatable("wld.status.illegal_uuid_format"));
            }
        } catch (Exception e) {
            MessageHelper.sendLogE(Component.translatable("wld.console.unexpected_error").getString(), e);
        }
        return Command.SINGLE_SUCCESS;
    }
}
