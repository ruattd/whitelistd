package io.github.ruattd.fc.whitelistd;

import lombok.NonNull;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;

public class MessageHelper {
    public static void sendSystemMessage(@NonNull CommandSource target, @NonNull Component message) {
        target.sendSystemMessage(Component.empty()
                .append(Component.empty().withStyle(ChatFormatting.DARK_AQUA).append("[Whitelistd] "))
                .append(message));
    }

    public static void sendSystemMessage(@NonNull Component message) {
        var server = Whitelistd.getInstance().getServer();
        if (server != null) sendSystemMessage(server, message);
    }
}
