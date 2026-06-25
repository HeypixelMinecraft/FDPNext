/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.render;

import java.awt.*;

public class ColorManager {

    public static int astolfoRainbow(int delay, int offset, int index) {
        double rainbowDelay = Math.ceil(System.currentTimeMillis() + (long)((long) delay * index)) / offset;
        return Color.getHSBColor((double)((float)((rainbowDelay %= 360.0) / 360.0)) < 0.5 ? -((float)(rainbowDelay / 360.0)) : (float)(rainbowDelay / 360.0), 0.5F, 1.0F).getRGB();
    }

}
