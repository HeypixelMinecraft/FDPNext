/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.ui.client.gui.clickgui.fonts.util;

public final class SneakyThrowing {

	public static RuntimeException sneakyThrow(Throwable throwable) {
		return sneakyThrow0(throwable);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> T sneakyThrow0(Throwable throwable) throws T {
		throw (T) throwable;
	}

	private SneakyThrowing() {
		throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
	}
}
