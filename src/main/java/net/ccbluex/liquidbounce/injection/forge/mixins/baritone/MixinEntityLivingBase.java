package net.ccbluex.liquidbounce.injection.forge.mixins.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.RotationMoveEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/**
 * @author Brady
 * @since 9/10/2018
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends Entity {

    public MixinEntityLivingBase(World worldIn) {
        super(worldIn);
    }

    @Redirect(
            method = "moveEntityWithHeading",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;moveFlying(FFF)V"
            )
    )
    private void onMoveRelative(EntityLivingBase instance, float strafe, float forward, float friction) {
        Optional<IBaritone> baritone = this.getBaritone();
        if (!baritone.isPresent()) {
            moveFlying(strafe, forward, friction);
            return;
        }

        RotationMoveEvent event = new RotationMoveEvent(RotationMoveEvent.Type.MOTION_UPDATE, this.rotationYaw, this.rotationPitch);
        baritone.get().getGameEventHandler().onPlayerRotationMove(event);

        this.rotationYaw = event.getYaw();
        this.rotationPitch = event.getPitch();

        this.moveFlying(strafe, forward, friction);

        this.rotationYaw = event.getOriginal().getYaw();
        this.rotationPitch = event.getOriginal().getPitch();
    }

    @Unique
    private Optional<IBaritone> getBaritone() {
        // noinspection ConstantConditions
        if (EntityPlayerSP.class.isInstance(this)) {
            return Optional.ofNullable(BaritoneAPI.getProvider().getBaritoneForPlayer((EntityPlayerSP) (Object) this));
        } else {
            return Optional.empty();
        }
    }
}
