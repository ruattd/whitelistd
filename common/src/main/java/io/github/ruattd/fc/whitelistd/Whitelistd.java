package io.github.ruattd.fc.whitelistd;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import lombok.NonNull;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class Whitelistd {
	/**
	 * Mod ID (whitelistd)
	 */
	public static final String MOD_ID = "whitelistd";

	/**
	 * Config object for global mod
	 */
	@NonNull
	public final WhitelistdConfig modConfig;

	private static Whitelistd instance;

	@NonNull
	public static Whitelistd instance() {
		return instance;
	}

	@NonNull
	private final Path configFile;

	private void writeConfigFile(Gson gson) throws IOException {
		var writer = new JsonWriter(Files.newBufferedWriter(configFile));
		writer.setIndent("  ");
		gson.toJson(modConfig, WhitelistdConfig.class, writer);
		writer.close();
	}

	public Whitelistd() {
		if (instance != null) throw new RuntimeException("Only one main instance can be created");
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
		init();
	}

	private void init() {
		//TODO load config
		if ((!modConfig.bypassClientCheck) && (Platform.getEnvironment() == Env.CLIENT)) {
			var showWarning = new AtomicBoolean(true);
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
