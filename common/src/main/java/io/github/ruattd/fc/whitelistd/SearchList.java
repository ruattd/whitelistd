package io.github.ruattd.fc.whitelistd;

public interface SearchList {
    /**
     * 搜索列表的初始化回调方法，将在插件加载时调用
     * @param mode 搜索模式，参考 {@link SearchMode}
     * @param strictName 维护列表时是否允许重名：值为 {@code true} 则不允许重名
     */
    void init(SearchMode mode, boolean strictName);

    /**
     * 向搜索列表中添加项目
     * @param player 玩家信息
     * @return 添加操作的结果，参考 {@link AddItemState}
     */
    AddItemState addItem(PlayerInfo... player);

    /**
     * 从搜索列表中移除指定项目
     * @param player 玩家信息
     * @return 移除操作的结果，参考 {@link RemoveItemState}
     */
    RemoveItemState removeItem(PlayerInfo player);

    /**
     * 查询搜索列表
     * @param player 玩家信息
     * @return 该玩家是否存在于列表中：值为 {@code true} 则表示存在
     */
    boolean query(PlayerInfo player);

    enum AddItemState {
        SUCCESSFUL,
        DUPLICATE,
        IO_ERROR,
        NETWORK_ERROR,
        UNKNOWN_ERROR,
    }

    enum RemoveItemState {
        SUCCESSFUL,
        IO_ERROR,
        NETWORK_ERROR,
        UNKNOWN_ERROR,
    }
}
