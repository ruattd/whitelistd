package io.github.ruattd.fc.whitelistd;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.github.ruattd.fc.whitelistd.impl.HttpSearchList;
import io.github.ruattd.fc.whitelistd.impl.JsonSearchList;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import static net.minecraft.commands.Commands.*;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Whitelistd {
    /**
     * 日志
     */
    public static final Logger logger = LoggerFactory.getLogger("Whitelistd");

    /**
     * 模组 ID (whitelistd)
     */
    public static final String MOD_ID = "whitelistd";

    /**
     * 模组全部组件是否加载完成, {@code true} 表示已加载完成, 准备接受并验证玩家
     */
    @Getter
    private boolean ready = false;

    /**
     * 是否启用功能
     */
    @Getter @Setter
    private boolean enabled = true;

    /**
     * 是否无视白名单允许任何人加入
     * <p>
     * 此配置项仅供临时使用, 请勿随意修改
     */
    @Getter @Setter
    private boolean allowAll = false;

    /**
     * 模组全局配置
     * @see WhitelistdConfig
     */
    @NonNull @Getter
    private final WhitelistdConfig config;

    /**
     * 模组配置目录
     */
    @NonNull @Getter
    private final Path configDir;

    /**
     * Minecraft 服务器实例, 在 {@link LifecycleEvent#SERVER_BEFORE_START} 事件前为 {@code null}
     */
    @Getter
    private MinecraftServer server = null;

    /**
     * 服务端世界实例，在 {@link LifecycleEvent#SERVER_LEVEL_LOAD} 事件前为 {@code null}
     */
    @Getter
    private ServerLevel level = null;

    /**
     * 搜索列表，在 {@link LifecycleEvent#SERVER_STARTED} 事件前为 {@code null}
     */
    @Getter
    private SearchList searchList = null;

    /**
     * 本类单例，用于访问部分全局对象，一般不会获取到 {@code null}
     */
    @Getter
    private static Whitelistd instance = null;

    @NonNull
    private final Path configFile;

    public void writeConfigFile(@NonNull Gson gson) throws IOException {
        var writer = new JsonWriter(Files.newBufferedWriter(configFile));
        writer.setIndent("  ");
        gson.toJson(config, WhitelistdConfig.class, writer);
        writer.close();
    }

    private Whitelistd() {
        configDir = Platform.getConfigFolder().resolve("Whitelistd");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config directory", e);
        }
        configFile = configDir.resolve("config.json");
        Gson gson = new Gson();
        try {
            if (Files.exists(configFile)) {
                config = gson.fromJson(Files.newBufferedReader(configFile), WhitelistdConfig.class);
                writeConfigFile(gson);
            } else {
                config = new WhitelistdConfig();
                writeConfigFile(gson);
            }
        } catch (IOException e) {
            throw new WhitelistdRuntimeException("Failed to read/write config file", e);
        }
    }

    private static void init0() {
        if (instance != null) throw new WhitelistdRuntimeException("Main instance can only be initialized once");
        instance = new Whitelistd();
    }

    /**
     * 初始化方法，<font color="red">不要调用它</font>
     */
    public static void init() {
        // 初始化单例
        init0();
        // 注册事件
        final var instance = getInstance();
        final var config = instance.getConfig();
        LifecycleEvent.SERVER_LEVEL_LOAD.register(level -> instance.level = level);
        LifecycleEvent.SERVER_BEFORE_START.register(server -> instance.server = server);
        LifecycleEvent.SERVER_STARTING.register(server -> {
            MessageHelper.sendLogI("Start loading whitelist...");
            var mode = config.getStorageMode();
            MessageHelper.sendLogI("Current storage mode: " + mode);
            var args = config.getStorageArgs();
            if (args.length != mode.getArgNumber()) {
                MessageHelper.sendLogW("Invalid storage args length, this may cause problems.");
            }
            switch (mode) {
                case JSON -> instance.searchList = new JsonSearchList();
                case HTTP -> instance.searchList = new HttpSearchList();
                default -> throw new UnsupportedOperationException("Storage mode not implemented");
            }
            instance.searchList.init(config.getSearchMode(), config.isPlayerNameCaseSensitive(), args);
            instance.ready = true;
            MessageHelper.sendLogI("Finished loading whitelist");
        });
        // 注册指令
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
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
            var whitelistd = dispatcher.register(literal("whitelistd")
                    .requires(source -> source.hasPermission(instance.getConfig().getPermissionLevel()))
                    .then(literal("add")
                            .then(argument("name", string)
                                    .suggests(nameSuggest)
                                    .executes(context -> commandExecute(1, false, context))
                                    .then(argument("uuid", string)
                                            .executes(context -> commandExecute(1, true, context))
                                    )
                            )
                    )
                    .then(literal("remove")
                            .then(argument("name", string)
                                    .suggests(nameSuggest)
                                    .executes(context -> commandExecute(2, false, context))
                                    .then(argument("uuid", string)
                                            .executes(context -> commandExecute(2, true, context))
                                    )
                            )
                    )
                    .then(literal("query")
                            .then(argument("name", string)
                                    .suggests(nameSuggest)
                                    .executes(context -> commandExecute(3, false, context))
                                    .then(argument("uuid", string)
                                            .executes(context -> commandExecute(3, true, context))
                                    )
                            )
                    )
                    .then(literal("on")
                            .executes(context -> {
                                getInstance().setEnabled(true);
                                var source = context.getSource();
                                source.sendSystemMessage(Component.empty().append("Successfully enabled"));
                                MessageHelper.sendLogI(source.getTextName() + " enabled whitelist");
                                return Command.SINGLE_SUCCESS;
                            })
                    )
                    .then(literal("off")
                            .executes(context -> {
                                getInstance().setEnabled(false);
                                var source = context.getSource();
                                source.sendSystemMessage(Component.empty().append("Successfully disabled"));
                                MessageHelper.sendLogW(source.getTextName() + " disabled whitelist temporarily, vanilla whitelist is taking effect");
                                return Command.SINGLE_SUCCESS;
                            })
                    )
            );
            dispatcher.register(literal("wld").redirect(whitelistd));
        });
        // 客户端检测逻辑
        if ((!config.isDisableClientCheck()) && (Platform.getEnvironment() == Env.CLIENT)) {
            instance.setAllowAll(true);
            final var showWarning = new AtomicBoolean(true);
            PlayerEvent.PLAYER_JOIN.register(player -> {
                if (showWarning.get()) {
                    MessageHelper.sendSystemMessage(player, Component.empty().withStyle(ChatFormatting.RESET)
                            .append("This mod is only for server, please do NOT install it for your Minecraft client."));
                    MessageHelper.sendSystemMessage(player, Component.empty().withStyle(ChatFormatting.GOLD)
                            .append("Allow-all mode have been automatically enabled, or you will not be able to join the single player world. " +
                                    "If you believe it doesn't matter you can set 'disableClientCheck' to true in config.json"));
                    showWarning.set(false);
                }
            });
        }
    }

    private static int commandExecute(int operation, boolean existUuid, CommandContext<CommandSourceStack> context) {
        try {
            var source = context.getSource();
            var name = context.getArgument("name", String.class);
            if (name == null) {
                source.sendFailure(Component.empty().append("Player name must be specified"));
            } else {
                var uuid = existUuid ? context.getArgument("uuid", String.class) : null;
                try {
                    var playerInfo = new PlayerInfo(name, (uuid == null) ? null : UUID.fromString(uuid));
                    var searchList = getInstance().getSearchList();
                    var profileName = name + '{' + uuid + '}';
                    switch (operation) {
                        case 1 -> { // add
                            var state = searchList.addItem(playerInfo);
                            if (state == SearchList.AddItemState.SUCCESSFUL) {
                                source.sendSystemMessage(Component.empty().append("Successfully added"));
                                MessageHelper.sendLogI(source.getTextName() + " added " + profileName + " to whitelist");
                            } else {
                                source.sendFailure(Component.empty().append("Failed: " + state));
                            }
                        }
                        case 2 -> { // remove
                            var state = searchList.removeItem(playerInfo);
                            if (state == SearchList.RemoveItemState.SUCCESSFUL) {
                                source.sendSystemMessage(Component.empty().append("Successfully removed"));
                                MessageHelper.sendLogI(source.getTextName() + " removed " + profileName + " from whitelist");
                            } else {
                                source.sendFailure(Component.empty().append("Failed: " + state));
                            }
                        }
                        case 3 -> { // query
                            var result = searchList.query(playerInfo);
                            String message;
                            if (result.exist()) {
                                var stored = result.playerStored();
                                var resultName = stored.getName() + '{' + stored.getUuid() + '}';
                                message = "Found: " + resultName;
                            } else {
                                message = "Not found";
                            }
                            source.sendSystemMessage(Component.empty().append(message));
                        }
                    }
                } catch (IllegalArgumentException e) {
                    source.sendFailure(Component.empty().append("Illegal UUID format"));
                }
            }
        } catch (Exception e) {
            MessageHelper.sendLogE("An unexpected error occurred while executing command", e);
        }
        return Command.SINGLE_SUCCESS;
    }
}
