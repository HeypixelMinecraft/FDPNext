/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.client.gui.mainmenu.components

/**
 * Lays out the right-side content cards.
 * Positioned to the right of the sidebar.
 */
object MainMenuContentPanel {

    fun draw(startX: Float, startY: Float) {
        // Account card
        MainMenuAccountCard.draw(startX, startY)
    }

    fun totalWidth(): Float = MainMenuAccountCard.width()
    fun totalHeight(): Float = MainMenuAccountCard.height()
}
