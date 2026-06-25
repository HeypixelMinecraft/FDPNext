/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.client.gui.clickgui.style.styles.dropdown;

import net.ccbluex.liquidbounce.features.value.Value;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Manager {
    private static final List<Value> settingList = new CopyOnWriteArrayList<>();
    public static void put(Value setting) {
        settingList.add(setting);
    }
    public static List<Value> getSettingList() {
        return settingList;
    }

}
