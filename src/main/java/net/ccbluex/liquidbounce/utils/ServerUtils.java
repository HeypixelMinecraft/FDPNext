/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils;

import net.ccbluex.liquidbounce.FDPNext;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;

public final class ServerUtils extends MinecraftInstance {

    public static ServerData serverData;

    public static void connectToLastServer() {
        if(serverData == null)
            return;

        mc.displayGuiScreen(new GuiConnecting(new GuiMultiplayer(FDPNext.mainMenu), mc, serverData));
    }

    public static String getRemoteIp() {
        String serverIp = "Idling";

        if (mc.isIntegratedServerRunning()) {
            serverIp = "SinglePlayer";
        } else if (mc.theWorld != null && mc.theWorld.isRemote) {
            final ServerData serverData = mc.getCurrentServerData();
            if(serverData != null)
                serverIp = serverData.serverIP;
        }

        return serverIp;
    }
}