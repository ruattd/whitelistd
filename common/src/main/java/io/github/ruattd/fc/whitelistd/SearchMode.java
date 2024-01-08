package io.github.ruattd.fc.whitelistd;

/**
 * 查询搜索列表时使用的搜索模式, 仅影响查询操作, 不影响存储
 */
public enum SearchMode {
    /**
     * 依据名称进行搜索
     */
    PLAYER_NAME,

    /**
     * 依据 UUID 进行搜索
     */
    PLAYER_UUID,

    /**
     * 依据名称或 UUID 进行搜索，若查询时提供了 UUID 则会优先使用 UUID
     */
    PLAYER_NAME_OR_UUID,
}
