/*
 * FDPNext Hacked Client
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.patcher.bugfixes;

import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.client.audio.SoundList;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;
import java.util.Map;

@Mixin(SoundHandler.class)
public abstract class MixinSoundHandler {

    @Shadow
    protected abstract Map<String, SoundList> getSoundMap(InputStream stream);

    @Shadow
    public abstract SoundEventAccessorComposite getSound(ResourceLocation location);

    @Invoker("loadSoundResource")
    protected abstract void fdpnext$loadSoundResource(ResourceLocation location, SoundList soundList);

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void fdpnext$restoreVanillaSounds(CallbackInfo callbackInfo) {
        if (getSound(new ResourceLocation("dig.grass")) != null) {
            return;
        }

        InputStream stream = null;
        try {
            stream = SoundHandler.class.getResourceAsStream("/assets/minecraft/sounds.json");
            if (stream == null) {
                return;
            }

            for (Map.Entry<String, SoundList> entry : getSoundMap(stream).entrySet()) {
                fdpnext$loadSoundResource(new ResourceLocation("minecraft", entry.getKey()), entry.getValue());
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }
}
