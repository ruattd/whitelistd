package io.github.ruattd.fc.whitelistd;

import lombok.NonNull;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;

public class MessageHelper {
    public static void sendSystemMessage(@NonNull CommandSource target, @NonNull Component message) {
        target.sendSystemMessage(Component.empty()
                .append(Component.empty().withStyle(ChatFormatting.YELLOW).append("Whitelistd: "))
                .append(message));
    }

    public static void sendSystemMessage(@NonNull Component message) {
        sendSystemMessage(Whitelistd.getInstance().getServer(), message);
    }
}
