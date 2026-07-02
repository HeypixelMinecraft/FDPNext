/*
 * FDPNext Hacked Client
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.patcher.bugfixes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.client.audio.SoundList;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(SoundHandler.class)
public abstract class MixinSoundHandler {

    @Shadow
    protected abstract Map<String, SoundList> getSoundMap(InputStream stream);

    @Shadow
    public abstract SoundEventAccessorComposite getSound(ResourceLocation location);

    @Shadow
    private IResourceManager mcResourceManager;

    @Invoker("loadSoundResource")
    protected abstract void fdpnext$loadSoundResource(ResourceLocation location, SoundList soundList);

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void fdpnext$restoreVanillaSounds(CallbackInfo callbackInfo) {
        if (fdpnext$hasVanillaSounds()) {
            return;
        }

        fdpnext$restoreFromResourceManager();
        if (fdpnext$hasVanillaSounds()) {
            return;
        }

        fdpnext$restoreFromAssetIndex();
        if (fdpnext$hasVanillaSounds()) {
            return;
        }

        fdpnext$restoreFromClasspath();
    }

    private boolean fdpnext$hasVanillaSounds() {
        return getSound(new ResourceLocation("minecraft", "gui.button.press")) != null ||
                getSound(new ResourceLocation("minecraft", "dig.grass")) != null;
    }

    private void fdpnext$restoreFromResourceManager() {
        try {
            List<IResource> resources = mcResourceManager.getAllResources(new ResourceLocation("minecraft", "sounds.json"));
            for (IResource resource : resources) {
                InputStream stream = null;
                try {
                    stream = resource.getInputStream();
                    fdpnext$loadSoundMap(stream);
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            }
        } catch (Throwable ignored) {
            // Fall back to the Minecraft asset index below.
        }
    }

    private void fdpnext$restoreFromClasspath() {
        InputStream stream = null;
        try {
            stream = SoundHandler.class.getResourceAsStream("/assets/minecraft/sounds.json");
            if (stream == null) {
                return;
            }

            fdpnext$loadSoundMap(stream);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    private void fdpnext$restoreFromAssetIndex() {
        for (File assetsDir : fdpnext$assetDirectories()) {
            File index = new File(new File(assetsDir, "indexes"), "1.8.json");
            if (!index.isFile()) {
                continue;
            }

            InputStream stream = null;
            FileReader reader = null;
            try {
                reader = new FileReader(index);
                JsonObject objects = new JsonParser().parse(reader).getAsJsonObject().getAsJsonObject("objects");
                JsonObject soundJson = objects.getAsJsonObject("minecraft/sounds.json");
                if (soundJson == null) {
                    continue;
                }

                String hash = soundJson.get("hash").getAsString();
                File soundObject = new File(new File(new File(assetsDir, "objects"), hash.substring(0, 2)), hash);
                if (!soundObject.isFile()) {
                    continue;
                }

                stream = new FileInputStream(soundObject);
                fdpnext$loadSoundMap(stream);
                return;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                IOUtils.closeQuietly(reader);
                IOUtils.closeQuietly(stream);
            }
        }
    }

    private Set<File> fdpnext$assetDirectories() {
        Set<File> result = new LinkedHashSet<>();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.mcDataDir != null) {
            result.add(new File(minecraft.mcDataDir, "assets"));
            File parent = minecraft.mcDataDir.getParentFile();
            if (parent != null) {
                result.add(new File(parent, "assets"));
            }
        }

        String appData = System.getenv("APPDATA");
        if (appData != null) {
            result.add(new File(new File(appData, ".minecraft"), "assets"));
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            result.add(new File(new File(userHome, ".minecraft"), "assets"));
        }

        result.add(new File("D:\\.gradle\\caches\\minecraft\\assets"));
        return result;
    }

    private void fdpnext$loadSoundMap(InputStream stream) {
        for (Map.Entry<String, SoundList> entry : getSoundMap(stream).entrySet()) {
            fdpnext$loadSoundResource(new ResourceLocation("minecraft", entry.getKey()), entry.getValue());
        }
    }
}
