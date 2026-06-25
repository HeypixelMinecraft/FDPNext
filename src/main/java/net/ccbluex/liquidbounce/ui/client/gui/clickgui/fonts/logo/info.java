/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.client.gui.clickgui.fonts.logo;

import net.ccbluex.liquidbounce.ui.client.gui.clickgui.fonts.api.FontManager;
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.fonts.impl.SimpleFontManager;
import net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.tenacity.SideGui.SideGui;

public class info {
    public static String Name = "FDPNext";

    public static String version = "";
    public static String username;
    private final SideGui sideGui = new SideGui();
    private static info INSTANCE;
    public  SideGui getSideGui() {
        return sideGui;
    }
    public static info getInstance() {
        if (INSTANCE == null) INSTANCE = new info();
        return INSTANCE;
    }
    public static final FontManager fontManager = SimpleFontManager.create();
    public static FontManager getFontManager() {
        return fontManager;
    }
}