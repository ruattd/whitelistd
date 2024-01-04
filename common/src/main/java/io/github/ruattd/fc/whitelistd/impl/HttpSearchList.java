package io.github.ruattd.fc.whitelistd.impl;

import io.github.ruattd.fc.whitelistd.PlayerInfo;
import io.github.ruattd.fc.whitelistd.SearchList;
import io.github.ruattd.fc.whitelistd.SearchMode;
import lombok.NonNull;

public class HttpSearchList implements SearchList {
    @Override
    public void init(@NonNull SearchMode mode, boolean playerNameCaseSensitive, @NonNull String[] args) {
        //TODO
    }

    @Override @NonNull
    public AddItemState addItem(@NonNull PlayerInfo player) {
        //TODO
        return null;
    }

    @Override @NonNull
    public RemoveItemState removeItem(@NonNull PlayerInfo player) {
        //TODO
        return null;
    }

    @Override @NonNull
    public ClearState clear() {
        return null;
    }

    @Override @NonNull
    public QueryResult query(@NonNull PlayerInfo player) {
        //TODO
        return null;
    }
}
