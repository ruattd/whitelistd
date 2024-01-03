package io.github.ruattd.fc.whitelistd.fabric;

import io.github.ruattd.fc.whitelistd.Whitelistd;
import net.fabricmc.api.ModInitializer;

public class WhitelistdFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        new Whitelistd();
    }
}