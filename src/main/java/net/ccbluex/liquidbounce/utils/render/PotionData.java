/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.render;

public class PotionData {
    public int maxTimer = 0;
    public float animationX = 0;
    public final Translate translate;
    public final int level;
    public PotionData(Translate translate, int level) {
        this.translate = translate;
        this.level = level;
    }

    public float getAnimationX() {
        return animationX;
    }

    public int getMaxTimer() {
        return maxTimer;
    }
}