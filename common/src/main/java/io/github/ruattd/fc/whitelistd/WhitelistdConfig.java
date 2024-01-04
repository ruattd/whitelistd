package io.github.ruattd.fc.whitelistd;

import lombok.Data;

/**
 * 模组配置文件对应的数据类，用于直接序列化/反序列化
 */
@Data
public class WhitelistdConfig {
    /**
     * 禁用客户端检测, 该检测在模组初始化时执行,
     * 若为客户端则自动禁用部分功能以保证玩家可以正常加入单人游戏.
     */
    private final boolean disableClientCheck = false;

    /**
     * 搜索列表模式, 用于指定搜索列表存储和查询的依照物.
     * 例如设置为 PLAYER_UUID 时将会使用玩家的 UUID 来存储/查询搜索列表.
     * <p>
     * 目前支持: PLAYER_NAME, PLAYER_UUID, PLAYER_NAME_OR_UUID
     * @see SearchMode
     */
    private final SearchMode searchMode = SearchMode.PLAYER_NAME_OR_UUID;

    /**
     * 验证玩家名称时是否大小写敏感.
     * <p>
     * 警告: 该设置项为 false 时存储和验证时使用的名称均为纯小写,
     * 更改后可能会导致部分玩家白名单失效, 请酌情更改此项.
     */
    private final boolean playerNameCaseSensitive = true;

    /**
     * 存储模式, 用于指定搜索列表的具体实现.
     * <p>
     * 目前支持: JSON, HTTP
     * @see StorageMode
     */
    private final StorageMode storageMode = StorageMode.JSON;

    /**
     * 特定存储模式要求的参数
     * @see StorageMode
     */
    private final String[] storageArgs = {"whitelist.json"};

    /**
     * 启用 Record 功能.
     * <p>
     * 这个功能提供了 /record 指令,
     * 使用该指令添加一个不在线的用户名称时会将名称加入搜索列表,
     * 在使用该名称的用户第一次上线时自动记录 UUID.
     * <p>
     * 仅在搜索列表模式为 PLAYER_NAME_OR_UUID 时可以达到期望效果,
     * 否则实际效果将会因验证方式特点而有所区别.
     */
    private final boolean enableRecord = true; //TODO
}
