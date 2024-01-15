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
import java.util.function.Predicate;

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
            MessageHelper.sendLogI(Component.translatable("wld.log.start_loading").getString());
            var mode = config.getStorageMode();
            MessageHelper.sendLogI(Component.translatable("wld.log.storage_mode", mode.toString()).getString());
            var args = config.getStorageArgs();
            if (args.length != mode.getArgNumber()) {
                MessageHelper.sendLogW(Component.translatable("wld.console.invalid_storage_args_length").getString());
            }
            switch (mode) {
                case JSON -> instance.searchList = new JsonSearchList();
                case HTTP -> instance.searchList = new HttpSearchList();
                default -> throw new UnsupportedOperationException("Storage mode not implemented");
            }
            instance.searchList.init(config.getSearchMode(), config.isPlayerNameCaseSensitive(), args);
            instance.ready = true;
            MessageHelper.sendLogI(Component.translatable("wld.log.finished_loading").getString());
        });
        // 注册指令
        CommandRegistrationEvent.EVENT.register(Commands::register);
        // 客户端检测逻辑
        if ((!config.isDisableClientCheck()) && (Platform.getEnvironment() == Env.CLIENT)) {
            instance.setAllowAll(true);
            final var showWarning = new AtomicBoolean(true);
            PlayerEvent.PLAYER_JOIN.register(player -> {
                if (showWarning.get()) {
                    MessageHelper.sendSystemMessage(player, Component.translatable("wld.client.only_for_server").withStyle(ChatFormatting.RESET));
                    MessageHelper.sendSystemMessage(player, Component.translatable("wld.client.auto_allow_all").withStyle(ChatFormatting.GOLD));
                    showWarning.set(false);
                }
            });
        }
    }
}
