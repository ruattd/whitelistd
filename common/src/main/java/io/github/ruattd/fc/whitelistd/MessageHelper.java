package io.github.ruattd.fc.whitelistd;

import lombok.NonNull;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;

public class MessageHelper {
    public static void sendSystemMessage(@NonNull CommandSource target, @NonNull Component message) {
        target.sendSystemMessage(Component.empty()
                .append(Component.empty().withStyle(ChatFormatting.DARK_AQUA).append("Whitelistd: "))
                .append(message));
    }

    public static void sendSystemMessage(@NonNull Component message) {
        var server = Whitelistd.getInstance().getServer();
        if (server != null) sendSystemMessage(server, message);
    }

    public static void sendLogD(@NonNull String message) {
        Whitelistd.logger.debug(message);
    }

    public static void sendLogI(@NonNull String message) {
        Whitelistd.logger.info(message);
    }

    public static void sendLogW(@NonNull String message) {
        Whitelistd.logger.warn(message);
    }

    public static void sendLogE(@NonNull String message) {
        Whitelistd.logger.error(message);
    }

    public static void sendLogE(@NonNull String message, @NonNull Throwable throwable) {
        Whitelistd.logger.error(message, throwable);
    }
}
