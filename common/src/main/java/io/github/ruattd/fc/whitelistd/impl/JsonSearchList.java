package io.github.ruattd.fc.whitelistd.impl;

import io.github.ruattd.fc.whitelistd.PlayerInfo;
import io.github.ruattd.fc.whitelistd.SearchList;
import io.github.ruattd.fc.whitelistd.SearchMode;

public class JsonSearchList implements SearchList {
    private SearchMode mode;
    private boolean strictName;

    @Override
    public void init(SearchMode mode, boolean strictName) {
        this.mode = mode;
        this.strictName = strictName;
    }

    @Override
    public AddItemState addItem(PlayerInfo... player) {
        //TODO
        return AddItemState.SUCCESSFUL;
    }

    @Override
    public RemoveItemState removeItem(PlayerInfo player) {
        //TODO
        return RemoveItemState.SUCCESSFUL;
    }

    @Override
    public boolean query(PlayerInfo player) {
        //TODO
        return false;
    }
}
