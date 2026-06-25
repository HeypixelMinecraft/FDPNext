/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import net.ccbluex.liquidbounce.FDPNext;
import net.ccbluex.liquidbounce.features.module.modules.render.TrueSight;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WorldClient.class)
public class MixinWorldClient {

    @ModifyVariable(method = "doVoidFogParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;randomDisplayTick(Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V", shift = At.Shift.AFTER), ordinal = 0)
    private boolean handleBarriers(final boolean flag) {
        final TrueSight trueSight = FDPNext.moduleManager.getModule(TrueSight.class);
        return flag || trueSight.getState() && trueSight.getBarriersValue().get();
    }
}