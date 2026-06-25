/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.block;

import net.ccbluex.liquidbounce.FDPNext;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ViaVersionFix;
import net.minecraft.block.BlockLadder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.Objects;

@Mixin(BlockLadder.class)
public class MixinBlockLadder {
    @ModifyConstant(method = "setBlockBoundsBasedOnState", constant = @Constant(floatValue = 0.125F))
    private float ViaVersion_LadderBB(float constant) {
        if (Objects.requireNonNull(FDPNext.moduleManager.getModule(ViaVersionFix.class)).getState())
            return 0.1875F;
        return 0.125F;
    }
}
