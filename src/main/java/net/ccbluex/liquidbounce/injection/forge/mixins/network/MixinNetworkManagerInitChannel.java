/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 *
 * Attaches the ViaMCP pipeline to the vanilla client channel (NetworkManager's anonymous
 * ChannelInitializer) so non-proxy connections also get multi-version translation.
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.network;

import de.florianmichael.viamcp.MCPViaUtil;
import io.netty.channel.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.NetworkManager$5")
public class MixinNetworkManagerInitChannel {

    @Inject(method = "initChannel", at = @At("TAIL"), remap = false)
    private void onInitChannel(Channel channel, CallbackInfo ci) {
        MCPViaUtil.hookPipeline(channel);
    }
}
