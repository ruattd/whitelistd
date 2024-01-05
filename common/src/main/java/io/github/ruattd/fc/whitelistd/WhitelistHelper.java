package io.github.ruattd.fc.whitelistd;

import lombok.NonNull;

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
        if (result.exist() || (!config.isEnableRecord())) {
            return result;
        } else {
            // Record 实现
            var name = player.getName();
            var playerRecord = new PlayerInfo(name + ".record");
            var resultRecord = searchList.query(playerRecord);
            if (resultRecord.exist()) {
                searchList.addItem(player);
                searchList.removeItem(playerRecord);
                return new SearchList.QueryResult(true, name);
            } else {
                return result;
            }
        }
    }
}
