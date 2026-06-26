/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.client.gui.mainmenu.components

/**
 * Lays out the right-side content cards (account + community).
 * Positioned to the right of the sidebar.
 */
object MainMenuContentPanel {

    private val gap = 12f

    fun draw(startX: Float, startY: Float) {
        // Account card on top
        MainMenuAccountCard.draw(startX, startY)
        // Community card below, with gap
        MainMenuCommunityCard.draw(
            startX,
            startY + MainMenuAccountCard.height() + gap
        )
    }

    fun totalWidth(): Float = MainMenuAccountCard.width()
    fun totalHeight(): Float =
        MainMenuAccountCard.height() + gap + MainMenuCommunityCard.height()
}
