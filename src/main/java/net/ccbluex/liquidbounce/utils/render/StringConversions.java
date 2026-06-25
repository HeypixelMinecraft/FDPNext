/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.render;

public class StringConversions {
    public static Object castNumber(String newValueText) {
        if (newValueText.contains(".")) {
            if (newValueText.toLowerCase().contains("f")) {
                return (float) Float.parseFloat((String) newValueText);
            }
            return Double.parseDouble(newValueText);
        }
        if (StringConversions.isNumeric(newValueText)) {
            return Integer.parseInt(newValueText);
        }
        return newValueText;
    }

    public static boolean isNumeric(String text) {
        try {
            Integer.parseInt(text);
            return true;
        }
        catch (NumberFormatException numberFormatException) {
            return false;
        }
    }
}
