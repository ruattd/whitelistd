package io.github.ruattd.fc.whitelistd;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.github.ruattd.fc.whitelistd.impl.HttpSearchList;
import io.github.ruattd.fc.whitelistd.impl.JsonSearchList;
import lombok.Getter;
import lombok.NonNull;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Whitelistd {
	/**
	 * 模组 ID (whitelistd)
	 */
	public static final String MOD_ID = "whitelistd";

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
	 * Minecraft 服务器实例, 在 {@link LifecycleEvent#SERVER_STARTING} 事件前为 {@code null}
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
		LifecycleEvent.SERVER_STARTING.register(server -> {
			instance.server = server;
			MessageHelper.sendSystemMessage(server, Component.empty().append("Start loading whitelist..."));
			var mode = config.getStorageMode();
			MessageHelper.sendSystemMessage(server, Component.empty().append("Current storage mode: " + mode));
			var args = config.getStorageArgs();
			if (args.length != mode.getArgNumber()) {
				MessageHelper.sendSystemMessage(server, Component.empty().withStyle(ChatFormatting.GOLD)
						.append("Invalid storage args length, this may cause problems."));
			}
			switch (mode) {
				case JSON -> instance.searchList = new JsonSearchList();
				case HTTP -> instance.searchList = new HttpSearchList();
				default -> throw new UnsupportedOperationException("Storage mode not implemented");
			}
			instance.searchList.init(config.getSearchMode(), config.isPlayerNameCaseSensitive(), args);
		});
		// 客户端检测逻辑
		if ((!config.isDisableClientCheck()) && (Platform.getEnvironment() == Env.CLIENT)) {
			final var showWarning = new AtomicBoolean(true);
			PlayerEvent.PLAYER_JOIN.register(player -> {
				if (showWarning.get()) {
					MessageHelper.sendSystemMessage(player, Component.empty().withStyle(ChatFormatting.RESET)
							.append("This mod is only for server, please do NOT install it for your Minecraft client."));
					MessageHelper.sendSystemMessage(player, Component.empty().withStyle(ChatFormatting.GOLD)
							.append("Some features have been automatically disabled, or you will not be able to join the single player world. " +
									"If you believe it doesn't matter you can set 'disableClientCheck' to true in config.json"));
					showWarning.set(false);
				}
			});
		} else {
			//TODO hook player join
		}
	}
}
