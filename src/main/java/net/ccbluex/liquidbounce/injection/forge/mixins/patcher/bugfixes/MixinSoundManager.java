/*
 * ZAVZ Hacked Client
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.patcher.bugfixes;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paulscode.sound.SoundSystem;

import java.util.*;

@Mixin(SoundManager.class)
public abstract class MixinSoundManager {
    @Shadow public abstract boolean isSoundPlaying(ISound sound);

    @Shadow public abstract void reloadSoundSystem();

    @Shadow @Final private GameSettings options;

    @Shadow private boolean loaded;

    @Shadow @Final private Map<String, ISound> playingSounds;

    @Shadow @Final private Map<ISound, Integer> delayedSounds;

    @Shadow private int playTime;

    private final List<String> p_pausedSounds = new ArrayList<>();

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Redirect(
        method = "pauseAllSounds",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/SoundManager$SoundSystemStarterThread;pause(Ljava/lang/String;)V", remap = false)
    )
    private void p_onlyPauseSoundIfNecessary(@Coerce SoundSystem soundSystem, String sound) {
        if (isSoundPlaying(playingSounds.get(sound))) {
            soundSystem.pause(sound);
            p_pausedSounds.add(sound);
        }
    }

    @Redirect(
        method = "resumeAllSounds",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;", remap = false)
    )
    private Iterator<String> p_iterateOverPausedSounds(Set<String> keySet) {
        return p_pausedSounds.iterator();
    }

    @Inject(method = "setSoundCategoryVolume", at = @At("HEAD"))
    private void fdpnext$reloadSoundSystemWhenVolumeReturns(SoundCategory category, float volume, CallbackInfo callbackInfo) {
        if (!loaded && volume > 0.0F && options.getSoundLevel(SoundCategory.MASTER) > 0.0F) {
            reloadSoundSystem();
        }
    }

    @Inject(method = "playSound", at = @At("HEAD"))
    private void fdpnext$reloadSoundSystemBeforePlaying(ISound sound, CallbackInfo callbackInfo) {
        if (!loaded && options.getSoundLevel(SoundCategory.MASTER) > 0.0F) {
            reloadSoundSystem();
            delayedSounds.put(sound, playTime + 5);
        }
    }

    @Inject(method = "resumeAllSounds", at = @At("TAIL"))
    private void p_clearPausedSounds(CallbackInfo ci) {
        p_pausedSounds.clear();
    }
}
