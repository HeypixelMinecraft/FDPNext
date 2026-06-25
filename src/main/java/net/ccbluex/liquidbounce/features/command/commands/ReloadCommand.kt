/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.FDPNext
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.modules.misc.Insult
import net.ccbluex.liquidbounce.ui.cape.GuiCapeManager
import net.ccbluex.liquidbounce.ui.font.Fonts

class ReloadCommand : Command("reload", emptyArray()) {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        alert("Reloading...")
        alert("§c§lReloading commands...")
        FDPNext.commandManager = CommandManager()
        FDPNext.commandManager.registerCommands()
        FDPNext.isStarting = true
        FDPNext.isLoadingConfig = true
        FDPNext.scriptManager.disableScripts()
        FDPNext.scriptManager.unloadScripts()
        for (module in FDPNext.moduleManager.modules)
            FDPNext.moduleManager.generateCommand(module)
        alert("§c§lReloading scripts...")
        FDPNext.scriptManager.loadScripts()
        FDPNext.scriptManager.enableScripts()
        alert("§c§lReloading fonts...")
        Fonts.loadFonts()
        alert("§c§lReloading modules...")
        FDPNext.configManager.load(FDPNext.configManager.nowConfig, false)
        Insult.loadFile()
        GuiCapeManager.load()
        alert("§c§lReloading accounts...")
        FDPNext.fileManager.loadConfig(FDPNext.fileManager.accountsConfig)
        alert("§c§lReloading friends...")
        FDPNext.fileManager.loadConfig(FDPNext.fileManager.friendsConfig)
        alert("§c§lReloading xray...")
        FDPNext.fileManager.loadConfig(FDPNext.fileManager.xrayConfig)
        alert("§c§lReloading HUD...")
        FDPNext.fileManager.loadConfig(FDPNext.fileManager.hudConfig)
        alert("Reloaded.")
        FDPNext.isStarting = false
        FDPNext.isLoadingConfig = false
        System.gc()
    }
}
