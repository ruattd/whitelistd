package io.github.ruattd.fc.whitelistd;

import lombok.NonNull;

/**
 * 白名单工具类, 此类中绝大多数方法均需要在 {@link Whitelistd#isReady()} 为 {@code true} 后才可以调用
 */
public final class WhitelistHelper {
    /**
     * 带有 Record 功能的搜索列表查询静态方法
     * @param player 玩家信息
     * @return 查询结果
     */
    @NonNull
    public static SearchList.QueryResult query(PlayerInfo player) {
        var instance = Whitelistd.getInstance();
        if (!instance.isReady()) return SearchList.emptyResult(player); // 检查加载状态
        var config = instance.getConfig();
        var searchList = instance.getSearchList();
        var mode = config.getSearchMode();
        var uuid = player.getUuid();
        if (mode == SearchMode.PLAYER_UUID && uuid == null) {
            return SearchList.emptyResult(player);
        }
        var result = searchList.query(player);
        if ((!result.exist()) && config.isEnableRecord() && uuid != null) {
            // Record 实现
            var nameRecord = player.getName() + ".record";
            var playerRecord = new PlayerInfo(nameRecord);
            var resultRecord = searchList.query(playerRecord);
            if (resultRecord.exist()) {
                var addState = searchList.addItem(player);
                if (addState != SearchList.AddItemState.SUCCESSFUL) {
                    MessageHelper.sendLogE("Record add item failed: " + addState);
                } else {
                    var removeState = searchList.removeItem(playerRecord);
                    if (removeState != SearchList.RemoveItemState.SUCCESSFUL) {
                        MessageHelper.sendLogE("Record remove item failed: " + removeState);
                        MessageHelper.sendLogE("You need to remove '" + nameRecord + "' manually or it may cause unexpected problems");
                    }
                    return new SearchList.QueryResult(true, player);
                }
            }
        }
        return result;
    }
}
