/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.script

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.script.remapper.Remapper
import net.ccbluex.liquidbounce.utils.ClientUtils
import java.io.File

class ScriptManager {

    val scripts = mutableListOf<Script>()
    val scriptsFolder = File(FDPNext.fileManager.dir, "scripts")

    /**
     * Loads all scripts inside the scripts folder.
     */
    fun loadScripts() {
        if (!scriptsFolder.exists()) {
            scriptsFolder.mkdir()
        }

        scriptsFolder.listFiles()?.forEach {
            if (it.name.endsWith(".js", true)) {
                Remapper.loadSrg() // load SRG if needed, this will optimize the performance
                loadJsScript(it)
            }
        }
    }

    /**
     * Unloads all scripts.
     */
    fun unloadScripts() {
        scripts.clear()
    }

    fun unloadScript(script: Script) {
        scripts.remove(script)
    }

    fun loadScript(script: Script) {
        scripts.add(script)
    }

    /**
     * Loads a script from a file.
     */
    fun loadJsScript(scriptFile: File) {
        try {
            scripts.add(Script(scriptFile))
            ClientUtils.logInfo("[FDPScriptAPI] Successfully loaded script '${scriptFile.name}'.")
        } catch (t: Throwable) {
            ClientUtils.logError("[FDPScriptAPI] Failed to load script '${scriptFile.name}'.", t)
        }
    }

    /**
     * Enables all scripts.
     */
    fun enableScripts() {
        scripts.forEach { it.onEnable() }
    }

    /**
     * Disables all scripts.
     */
    fun disableScripts() {
        scripts.forEach { it.onDisable() }
    }
}