package io.github.ruattd.fc.whitelistd;

import lombok.*;

import java.util.UUID;

/**
 * 玩家信息，由名称和 UUID 两部分组成，其中名称一定存在，但 UUID 可能为 {@code null}
 */
@Data
public class PlayerInfo {
    public @NonNull String name;
    public UUID uuid;
}
