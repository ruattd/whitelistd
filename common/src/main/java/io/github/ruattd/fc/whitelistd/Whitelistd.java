package io.github.ruattd.fc.whitelistd;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import lombok.Getter;
import lombok.NonNull;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class Whitelistd {
	/**
	 * 模组 ID (whitelistd)
	 */
	public static final String MOD_ID = "whitelistd";

	/**
	 * 全局配置对象
	 */
	@NonNull @Getter
	private final WhitelistdConfig modConfig;

	/**
	 * 本类单例，用于访问部分全局对象，一般不会获取到 {@code null}
	 */
	@Getter
	private static Whitelistd instance;

	@NonNull
	private final Path configFile;

	private void writeConfigFile(Gson gson) throws IOException {
		var writer = new JsonWriter(Files.newBufferedWriter(configFile));
		writer.setIndent("  ");
		gson.toJson(modConfig, WhitelistdConfig.class, writer);
		writer.close();
	}

	private Whitelistd() {
		var configDir = Platform.getConfigFolder().resolve("Whitelistd");
		try {
			Files.createDirectories(configDir);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create config directory", e);
		}
		configFile = configDir.resolve("config.json");
		Gson gson = new Gson();
		try {
			if (Files.exists(configFile)) {
				modConfig = gson.fromJson(Files.newBufferedReader(configFile), WhitelistdConfig.class);
				writeConfigFile(gson);
			} else {
				modConfig = new WhitelistdConfig();
				writeConfigFile(gson);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to read/write config file", e);
		}
		instance = this;
	}

	/**
	 * 初始化方法，<font color="red">不要调用它</font>
	 */
	public static void init() {
		if (instance != null) throw new RuntimeException("Main instance can only be initialized once");
		instance = new Whitelistd();
		if ((!getInstance().modConfig.bypassClientCheck) && (Platform.getEnvironment() == Env.CLIENT)) {
			final var showWarning = new AtomicBoolean(true);
			PlayerEvent.PLAYER_JOIN.register(player -> {
				if (showWarning.get()) {
					player.sendSystemMessage(Component.empty()
							.append(Component.empty().withStyle(ChatFormatting.YELLOW).append("Whitelistd: "))
							.append(Component.empty().withStyle(ChatFormatting.RESET).append("This mod is only for server, please do NOT install it for your Minecraft client.")));
					player.sendSystemMessage(Component.empty()
							.append(Component.empty().withStyle(ChatFormatting.YELLOW).append("Whitelistd: "))
							.append(Component.empty().withStyle(ChatFormatting.GOLD).append("All features have been automatically disabled, or you will not be able to join the single player world. If you believe it doesn't matter you can set 'bypassClientCheck' to true in config.json")));
					showWarning.set(false);
				}
			});
		} else {
			//TODO hook player join
		}
	}
}
