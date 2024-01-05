package io.github.ruattd.fc.whitelistd;

import lombok.NonNull;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class WhitelistHelper {
    /**
     * 实现了 Record 功能的搜索列表查询静态方法
     * @param player 玩家信息
     * @return 查询结果
     */
    @NonNull
    public static SearchList.QueryResult query(PlayerInfo player) {
        var instance = Whitelistd.getInstance();
        var config = instance.getConfig();
        var searchList = instance.getSearchList();
        var mode = config.getSearchMode();
        if (mode == SearchMode.PLAYER_UUID && player.getUuid() == null) {
            return new SearchList.QueryResult(false, player.getName());
        }
        var result = searchList.query(player);
        if ((!result.exist()) || config.isEnableRecord()) {
            // Record 实现
            var name = player.getName();
            var nameRecord = name + ".record";
            var playerRecord = new PlayerInfo(nameRecord);
            var resultRecord = searchList.query(playerRecord);
            if (resultRecord.exist()) {
                var addState = searchList.addItem(player);
                if (addState != SearchList.AddItemState.SUCCESSFUL) {
                    MessageHelper.sendSystemMessage(Component.empty().withStyle(ChatFormatting.RED)
                            .append("Record add item failed: " + addState));
                } else {
                    var removeState = searchList.removeItem(playerRecord);
                    if (removeState != SearchList.RemoveItemState.SUCCESSFUL) {
                        MessageHelper.sendSystemMessage(Component.empty().withStyle(ChatFormatting.RED)
                                .append("Record remove item failed: " + removeState));
                        MessageHelper.sendSystemMessage(Component.empty().withStyle(ChatFormatting.GOLD)
                                .append("You need to remove '" + nameRecord + "' manually or it may cause unexpected problems"));
                    }
                    return new SearchList.QueryResult(true, name);
                }
            }
        }
        return result;
    }
}
