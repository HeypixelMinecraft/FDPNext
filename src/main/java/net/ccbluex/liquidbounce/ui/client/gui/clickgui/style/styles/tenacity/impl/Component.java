/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.tenacity.impl;

import net.minecraft.client.Minecraft;
public abstract class Component {

    public Minecraft mc = Minecraft.getMinecraft();

    abstract public void initGui();

    public abstract void keyTyped(char typedChar, int keyCode);

    abstract public void drawScreen(int mouseX, int mouseY);

    abstract public void mouseClicked(int mouseX, int mouseY, int button);

    abstract public void mouseReleased(int mouseX, int mouseY, int state);
}
