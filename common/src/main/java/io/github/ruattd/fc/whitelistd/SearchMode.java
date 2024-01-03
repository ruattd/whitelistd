package io.github.ruattd.fc.whitelistd;

/**
 * 查询搜索列表时使用的搜索模式，目前可用两种方式：依据名称 或 依据 UUID
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
}
