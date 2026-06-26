/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.patcher.bugfixes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes music continuing to play after the player has disabled the
 * MUSIC sound category (volume = 0). Vanilla only checks whether the
 * sound is still "playing" via the sound handler, which stays true even
 * when the category volume is zero, so the track never gets stopped and
 * new tracks keep being scheduled. We intercept {@link MusicTicker#update()}
 * and bail out early when the category is muted, stopping any active track.
 */
@Mixin(MusicTicker.class)
public abstract class MixinMusicTicker {

    @Shadow
    private Minecraft mc;

    @Shadow
    private ISound currentMusic;

    @Inject(method = "update()V", at = @At("HEAD"), cancellable = true)
    private void fdpnext$stopMusicWhenMuted(CallbackInfo ci) {
        GameSettings settings = this.mc.gameSettings;
        if (settings == null) {
            return;
        }
        if (settings.getSoundLevel(SoundCategory.MUSIC) <= 0.0F) {
            if (this.currentMusic != null) {
                SoundHandler soundHandler = this.mc.getSoundHandler();
                if (soundHandler != null) {
                    soundHandler.stopSound(this.currentMusic);
                }
                this.currentMusic = null;
            }
            ci.cancel();
        }
    }
}
