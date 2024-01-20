package io.github.ruattd.fc.whitelistd;

import lombok.NonNull;

import java.util.Iterator;
import java.util.function.Predicate;

public interface SearchList {
    /**
     * 搜索列表的初始化回调方法，将在插件加载时调用
     * @param mode 搜索模式，参考 {@link SearchMode}
     * @param playerNameCaseSensitive 玩家名称大小写是否敏感，一般不需要关心
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
     * @param playerStored 搜索列表中存储的玩家信息
     */
    record QueryResult(boolean exist, PlayerInfo playerStored) {}

    static QueryResult emptyResult(PlayerInfo player) {
        return new QueryResult(false, player);
    }

    /**
     * 获取列表项目总数
     * @return 项目总数
     */
    int size();

    /**
     * 获取所有项目
     * @return 所有项目
     */
    @NonNull
    Iterable<PlayerInfo> getItems();

    /**
     * 获取所有项目并筛选需要的项目
     * @param filter 筛选器
     * @return 筛选结果
     */
    @NonNull
    default Iterable<PlayerInfo> getItems(@NonNull Predicate<PlayerInfo> filter) {
        return () -> new Iterator<>() {
            private PlayerInfo next = null;
            private final Iterator<PlayerInfo> allItems = getItems().iterator();

            @Override
            public boolean hasNext() {
                while (this.next == null) {
                    if (allItems.hasNext()) {
                        var next = allItems.next();
                        if (filter.test(next)) this.next = next;
                    } else {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public PlayerInfo next() {
                return next;
            }
        };
    }

    /**
     * 以索引位置获取多个项目
     * @param firstIndex 开始位置
     * @param lastIndex 结束位置
     * @return 指定项目
     * @throws UnsupportedOperationException 搜索列表未实现此功能
     * @see SearchList#getItems(int)
     */
    @NonNull
    default Iterable<PlayerInfo> getItems(int firstIndex, int lastIndex) {
        throw new UnsupportedOperationException();
    }

    /**
     * 以索引位置获取多个项目, 为 {@link SearchList#getItems(int, int)} 的重载,
     * 默认使用末位索引作为 {@code lastIndex}
     * @param firstIndex 开始位置
     * @return 指定项目
     * @throws UnsupportedOperationException 搜索列表未实现此功能
     */
    @NonNull
    default Iterable<PlayerInfo> getItems(int firstIndex) {
        return getItems(firstIndex, size() - 1);
    }
}
