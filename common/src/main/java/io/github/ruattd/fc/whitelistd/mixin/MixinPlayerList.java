package io.github.ruattd.fc.whitelistd.mixin;

import com.mojang.authlib.GameProfile;
import io.github.ruattd.fc.whitelistd.MessageHelper;
import io.github.ruattd.fc.whitelistd.PlayerInfo;
import io.github.ruattd.fc.whitelistd.WhitelistHelper;
import io.github.ruattd.fc.whitelistd.Whitelistd;
import lombok.NonNull;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dedicated.DedicatedPlayerList;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedPlayerList.class)
public abstract class MixinPlayerList {
    @Inject(at = @At("HEAD"), method = "isWhiteListed(Lcom/mojang/authlib/GameProfile;)Z", cancellable = true)
    public void isWhiteListed(GameProfile profile, CallbackInfoReturnable<Boolean> returnable) {
        var instance = Whitelistd.getInstance();
        var name = profile.getName();
        var uuid = profile.getId();
        var profileString = name + '{' + uuid + '}';
        MessageHelper.sendSystemMessage(Component.empty().append("Whitelist request: " + profileString));
        String message;
        boolean returnValue;
        if (instance.isReady()) {
            if (instance.isAllowAll()) {
                message = "Allowed " + name + " (allow-all mode)";
                returnValue = true;
            } else {
                if (name == null) {
                    message = "Rejected (name is null)";
                    returnValue = false;
                } else {
                    var result = WhitelistHelper.query(new PlayerInfo(name, uuid));
                    returnValue = result.exist();
                    String s;
                    if (returnValue) s = "Allowed"; else s = "Rejected";
                    message = s + ' ' + name;
                }
            }
            MessageHelper.sendSystemMessage(Component.empty().append(message));
            returnable.setReturnValue(returnValue);
        }
    }
}