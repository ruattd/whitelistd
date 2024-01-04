package io.github.ruattd.fc.whitelistd;

import lombok.NonNull;

public interface SearchList {
    /**
     * 搜索列表的初始化回调方法，将在插件加载时调用
     * @param mode 搜索模式，参考 {@link SearchMode}
     * @param playerNameCaseSensitive 维护列表时是否允许重名：值为 {@code true} 则不允许重名
     */
    void init(@NonNull SearchMode mode, boolean playerNameCaseSensitive, @NonNull String[] args);

    /**
     * 向搜索列表中添加项目
     * @param player 玩家信息
     * @return 添加操作的结果
     * @see AddItemState
     */
    @NonNull
    AddItemState addItem(@NonNull PlayerInfo player);

    /**
     * 从搜索列表中移除指定项目
     * @param player 玩家信息
     * @return 移除操作的结果
     * @see RemoveItemState
     */
    @NonNull
    RemoveItemState removeItem(@NonNull PlayerInfo player);

    /**
     * 查询搜索列表: 完整查询
     * @param player 玩家信息
     * @return 查询结果
     * @see QueryResult
     */
    @NonNull
    QueryResult query(@NonNull PlayerInfo player);

    /**
     * 清空搜索列表，删除所有玩家信息
     * @return 清空操作的结果
     * @see ClearState
     */
    @NonNull
    ClearState clear();

    enum AddItemState {
        SUCCESSFUL,
        DUPLICATE,
        IO_ERROR,
        NETWORK_ERROR,
        UNKNOWN_ERROR,
    }

    enum RemoveItemState {
        SUCCESSFUL,
        NOT_FOUND,
        IO_ERROR,
        NETWORK_ERROR,
        UNKNOWN_ERROR,
    }

    enum ClearState {
        SUCCESSFUL,
        IO_ERROR,
        NETWORK_ERROR,
        UNKNOWN_ERROR,
    }

    /**
     * 查询结果
     * @param exist 玩家是否存在于搜索列表中: {@code true} 为存在
     * @param playerName 搜索列表中存储的名字
     */
    record QueryResult(boolean exist, String playerName) {}
}
