/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.event

import java.lang.reflect.Method

interface Listenable {
    fun handleEvents(): Boolean
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventTarget(val ignoreCondition: Boolean = false)

internal class EventHook(val eventClass: Listenable, val method: Method, eventTarget: EventTarget) {
    val isIgnoreCondition = eventTarget.ignoreCondition
}
