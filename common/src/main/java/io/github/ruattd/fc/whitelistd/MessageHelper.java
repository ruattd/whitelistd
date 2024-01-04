package io.github.ruattd.fc.whitelistd;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;

public class MessageHelper {
    public static void sendSystemMessage(CommandSource target, Component message) {
        target.sendSystemMessage(Component.empty()
                .append(Component.empty().withStyle(ChatFormatting.YELLOW).append("Whitelistd: "))
                .append(message));
    }
}
