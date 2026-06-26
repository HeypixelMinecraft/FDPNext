/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.client.gui.mainmenu

/**
 * Side bar button data model for the modern main menu.
 *
 * @param id     stable identifier used for selection persistence
 * @param icon   single-char icon shown on the left of the button
 * @param i18nKey language key (without surrounding %) used for the label
 * @param action invoked on left click
 */
data class MainMenuButton(
    val id: String,
    val icon: String,
    val i18nKey: String,
    val action: () -> Unit
)
