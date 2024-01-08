package io.github.ruattd.fc.whitelistd;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 存储搜索列表数据的方式
 */
@Getter @RequiredArgsConstructor
public enum StorageMode {
    /**
     * 使用 JSON 文件存储数据, 此方法是最简单且最便于迁移的, 但是性能最差.
     * 由于使用 JSON 存储, 需要在服务器内存中临时保存所有信息, 且会大量读写单个文件, 故不适用于大型服务器.
     * <p>
     * [参数] (1)<br/>
     * JSON 文件位置 (相对于 config 目录)
     * <p>
     * [可重复性]<br/>
     * 不支持重复名称<br/>
     * 不支持重复 UUID
     */
    JSON(1),

    /**
     * 使用 MySQL 存储数据, 此方法性能较好, 但依赖 MySQL 环境.
     * <p>
     * [参数] (?)<br/>
     */
    MYSQL(0),

    /**
     * 使用 MongoDB 存储数据, 此方法性能较好, 但依赖 MongoDB 环境,
     * 这个环境可以在任何可访问的位置, 推荐部署在本地.
     * <p>
     * [参数] (3)<br/>
     * 可访问的 MongoDB Connection URI, 数据库名, 数据集名
     */
    MONGODB(3),

    /**
     * 使用 HTTP API 存储数据, 此方法适用于模块化部署, 需要提供一个专用于搜索列表存储的 HTTP API.
     * 这种方法的性能完全取决于网络和提供的 API 本身.
     * <p>
     * [参数] (3)<br/>
     * 用于 API 请求的 URL, 包含查找/添加/移除三种.
     * URL 中的 %p 将被替换为玩家名称, %u 将被替换为玩家 UUID.
     * 查找 API 将接受 GET 请求, 并返回一个布尔值 (true/false, 不区分大小写) 代表结果;
     * 添加/移除 API 将接受 POST 请求, 不需要返回任何内容 (即使有内容也会被忽略),
     * 使用 HTTP 状态码来表示成功/失败即可.
     */
    HTTP(3),

    ;

    private final int argNumber;
}
