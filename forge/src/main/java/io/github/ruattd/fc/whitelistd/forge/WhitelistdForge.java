package io.github.ruattd.fc.whitelistd.forge;

import dev.architectury.platform.forge.EventBuses;
import io.github.ruattd.fc.whitelistd.Whitelistd;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Whitelistd.MOD_ID)
public class WhitelistdForge {
    public WhitelistdForge() {
		// Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(Whitelistd.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        Whitelistd.init();
    }
}