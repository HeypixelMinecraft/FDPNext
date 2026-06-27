/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce

import net.ccbluex.liquidbounce.event.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.macro.MacroManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.special.*
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.file.config.ConfigManager
import net.ccbluex.liquidbounce.ui.client.gui.EnumLaunchFilter
import net.ccbluex.liquidbounce.ui.client.gui.LaunchFilterInfo
import net.ccbluex.liquidbounce.ui.client.gui.LaunchOption
import net.ccbluex.liquidbounce.ui.client.gui.GuiLaunchOptionSelectMenu
import net.ccbluex.liquidbounce.ui.client.gui.scriptOnline.ScriptSubscribe
import net.ccbluex.liquidbounce.ui.client.gui.scriptOnline.Subscriptions
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.ui.cape.GuiCapeManager
import net.ccbluex.liquidbounce.ui.client.hud.HUD
import net.ccbluex.liquidbounce.ui.client.keybind.KeyBindManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.i18n.LanguageManager
import net.ccbluex.liquidbounce.ui.sound.TipSoundManager
import net.ccbluex.liquidbounce.utils.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.ResourceLocation
import java.util.*

object FDPNext {

    // Client information
    const val CLIENT_NAME = "FDPNext"
    const val COLORED_NAME = "§7[§b§lFDPNext§7] "
    const val CLIENT_CREATOR = "Skidder"
    const val CLIENT_WEBSITE = "fdpinfo.github.io"
    const val CLIENT_VERSION = "v0.1.0-beta"

    // Flags
    var isStarting = true
    var isLoadingConfig = true

    @JvmField
    val gitInfo = Properties().also {
        val inputStream = FDPNext::class.java.classLoader.getResourceAsStream("git.properties")
        if (inputStream != null) {
            it.load(inputStream)
        } else {
            it["git.branch"] = "Main"
        }
    }

    @JvmField
    val CLIENT_BRANCH = (gitInfo["git.branch"] ?: "unknown").let {
        if (it == "main") "Main" else it
    }

    @JvmField
    val CLIENT_COMMIT = (gitInfo["git.commit.id.abbrev"] as? String)
        ?: (gitInfo["git.commit.id"] as? String)?.substring(0, 7)
        ?: "unknown"

    // Managers
    lateinit var moduleManager: ModuleManager
    lateinit var commandManager: CommandManager
    lateinit var eventManager: EventManager
    private lateinit var subscriptions: Subscriptions
    lateinit var fileManager: FileManager
    lateinit var scriptManager: ScriptManager
    lateinit var tipSoundManager: TipSoundManager
    lateinit var combatManager: CombatManager
    lateinit var macroManager: MacroManager
    lateinit var configManager: ConfigManager

    // Shared state holders
    val shitCode = net.ccbluex.liquidbounce.utils.ShitCode()

    // Some UI things
    lateinit var hud: HUD
    lateinit var mainMenu: GuiScreen
    lateinit var keyBindManager: KeyBindManager

    // Menu Background
    var background: ResourceLocation? = ResourceLocation("FDPNext/background.png")

    val launchFilters = mutableListOf<EnumLaunchFilter>()
    private val dynamicLaunchOptions: Array<LaunchOption>
        get() = ClassUtils.resolvePackage(
            "${LaunchOption::class.java.`package`.name}.options",
            LaunchOption::class.java
        )
            .filter {
                val annotation = it.getDeclaredAnnotation(LaunchFilterInfo::class.java)
                if (annotation != null) {
                    return@filter annotation.filters.toMutableList() == launchFilters
                }
                false
            }
            .map {
                try {
                    it.newInstance()
                } catch (e: IllegalAccessException) {
                    ClassUtils.getObjectInstance(it) as LaunchOption
                }
            }.toTypedArray()

    /**
     * Execute if client will be started
     */
    fun initClient() {
        ClientUtils.logInfo("Loading $CLIENT_NAME $CLIENT_VERSION")
        ClientUtils.logInfo("Initializing...")
        val startTime = System.currentTimeMillis()

        // Initialize managers
        fileManager = FileManager()
        configManager = ConfigManager()
        subscriptions = Subscriptions()
        eventManager = EventManager()
        commandManager = CommandManager()
        macroManager = MacroManager()
        moduleManager = ModuleManager()
        scriptManager = ScriptManager()
        keyBindManager = KeyBindManager()
        combatManager = CombatManager()
        tipSoundManager = TipSoundManager()

        // Initialize built-in ViaMCP (Java multi-version: ViaVersion + ViaBackwards + ViaRewind)
        try {
            de.florianmichael.viamcp.ViaMCP.create()
            de.florianmichael.viamcp.ViaMCP.INSTANCE.initAsyncSlider(148, 8, 110, 20)
        } catch (t: Throwable) {
            ClientUtils.logError("Failed to initialize ViaMCP (multi-version)", t)
        }

        // Load language
        LanguageManager.switchLanguage(Minecraft.getMinecraft().gameSettings.language)

        // Register listeners
        eventManager.registerListener(RotationUtils())
        eventManager.registerListener(ClientFixes)
        eventManager.registerListener(ClientSpoof())
        eventManager.registerListener(InventoryUtils)
        eventManager.registerListener(BungeeCordSpoof())
        eventManager.registerListener(ServerSpoof)
        eventManager.registerListener(SessionUtils())
        eventManager.registerListener(StatisticsUtils())
        eventManager.registerListener(LocationCache())
        eventManager.registerListener(macroManager)
        eventManager.registerListener(combatManager)

        // Setup modules first (required before loading configs that reference modules like XRay)
        moduleManager.registerModules()

        // Load configs
        fileManager.loadConfigs(
            fileManager.accountsConfig,
            fileManager.friendsConfig,
            fileManager.specialConfig,
            fileManager.subscriptsConfig,
            fileManager.hudConfig,
            fileManager.xrayConfig
        )

        // Load client fonts
        Fonts.loadFonts()

        // Load and enable scripts
        try {
            scriptManager.loadScripts()
            scriptManager.enableScripts()
        } catch (throwable: Throwable) {
            ClientUtils.logError("Failed to load scripts.", throwable)
        }

        // Register commands
        commandManager.registerCommands()

        // Load GUI
        GuiCapeManager.load()
        mainMenu = GuiLaunchOptionSelectMenu()
        hud = HUD.createDefault()

        // Load script subscripts
        ClientUtils.logInfo("Loading Script Subscripts...")
        fileManager.subscriptsConfig.subscripts.forEach { subscript ->
            Subscriptions.addSubscribes(ScriptSubscribe(subscript.url, subscript.name))
        }
        scriptManager.disableScripts()
        scriptManager.unloadScripts()
        Subscriptions.subscribes.forEach { scriptSubscribe ->
            scriptSubscribe.load()
        }
        scriptManager.loadScripts()
        scriptManager.enableScripts()

        // Set title
        ClientUtils.setTitle()

        // Log success
        ClientUtils.logInfo("$CLIENT_NAME $CLIENT_VERSION loaded in ${(System.currentTimeMillis() - startTime)}ms!")
    }

    /**
     * Execute if client ui type is selected
     */
    // Start dynamic launch options
    fun startClient() {
        dynamicLaunchOptions.forEach {
            it.start()
        }

        // Load configs
        configManager.loadLegacySupport()
        configManager.loadConfigSet()

        // Set is starting status
        isStarting = false
        isLoadingConfig = false

        ClientUtils.logInfo("$CLIENT_NAME $CLIENT_VERSION started!")
    }

    /**
     * Execute if client will be stopped
     */
    fun stopClient() {
        // Check if client is not starting or loading configurations
        if (!isStarting && !isLoadingConfig) {
            ClientUtils.logInfo("Shutting down $CLIENT_NAME $CLIENT_VERSION!")

            // Call client shutdown
            eventManager.callEvent(ClientShutdownEvent())

            // Save configurations
            GuiCapeManager.save() // Save capes
            configManager.save(true, forceSave = true) // Save configs
            fileManager.saveAllConfigs() // Save file manager configs

            // Stop dynamic launch options
            dynamicLaunchOptions.forEach {
                it.stop()
            }
        }
    }
}
