/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.utils.autoplayrobot

import net.minecraft.client.Minecraft
import net.minecraft.util.BlockPos

object BaritoneBridge {
    private var initialized = false
    private var available = false
    private var failureReason = "Not checked"

    private var apiClass: Class<*>? = null
    private var goalBlockClass: Class<*>? = null

    fun isAvailable(): Boolean {
        ensureInitialized()
        return available
    }

    fun getFailureReason(): String {
        ensureInitialized()
        return failureReason
    }

    fun goto(pos: BlockPos): Boolean {
        ensureInitialized()
        if (!available) {
            return false
        }

        return runCatching {
            val baritone = primaryBaritone() ?: return false
            val goalProcess = invokeNoArg(baritone, "getCustomGoalProcess") ?: return false
            val goal = goalBlockClass!!.getConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                .newInstance(pos.x, pos.y, pos.z)

            val setGoalAndPath = goalProcess.javaClass.methods.firstOrNull {
                it.name == "setGoalAndPath" && it.parameterTypes.size == 1
            }

            if (setGoalAndPath != null) {
                setGoalAndPath.invoke(goalProcess, goal)
                true
            } else {
                val setGoal = goalProcess.javaClass.methods.firstOrNull {
                    it.name == "setGoal" && it.parameterTypes.size == 1
                } ?: return false
                setGoal.invoke(goalProcess, goal)
                invokeNoArg(goalProcess, "path")
                true
            }
        }.getOrElse {
            failureReason = it.javaClass.simpleName + ": " + (it.message ?: "goto failed")
            false
        }
    }

    fun stop() {
        ensureInitialized()
        if (!available) {
            return
        }

        runCatching {
            val baritone = primaryBaritone() ?: return@runCatching
            invokeNoArg(invokeNoArg(baritone, "getPathingBehavior") ?: baritone, "cancelEverything")
            invokeNoArg(invokeNoArg(baritone, "getCustomGoalProcess") ?: baritone, "onLostControl")
        }
    }

    fun isPathing(): Boolean {
        ensureInitialized()
        if (!available) {
            return false
        }

        return runCatching {
            val baritone = primaryBaritone() ?: return false
            val pathingBehavior = invokeNoArg(baritone, "getPathingBehavior") ?: return false
            (invokeNoArg(pathingBehavior, "isPathing") as? Boolean) ?: false
        }.getOrDefault(false)
    }

    fun clearGoal() {
        stop()
    }

    private fun ensureInitialized() {
        if (initialized) {
            return
        }
        initialized = true

        runCatching {
            apiClass = Class.forName("baritone.api.BaritoneAPI")
            goalBlockClass = Class.forName("baritone.api.pathing.goals.GoalBlock")
            val baritone = primaryBaritone()
            if (baritone == null) {
                failureReason = "BaritoneAPI provider returned no primary Baritone"
                available = false
            } else {
                available = true
                failureReason = "OK"
            }
        }.onFailure {
            available = false
            failureReason = it.javaClass.simpleName + ": " + (it.message ?: "Baritone API not found")
        }
    }

    private fun primaryBaritone(): Any? {
        val api = apiClass ?: return null
        val provider = api.getMethod("getProvider").invoke(null) ?: return null
        val primary = runCatching { invokeNoArg(provider, "getPrimaryBaritone") }.getOrNull()
        if (primary != null) {
            return primary
        }

        val createBaritone = provider.javaClass.methods.firstOrNull {
            it.name == "createBaritone" && it.parameterTypes.size == 1 && it.parameterTypes[0].isAssignableFrom(Minecraft::class.java)
        } ?: return null

        return createBaritone.invoke(provider, Minecraft.getMinecraft())
    }

    private fun invokeNoArg(target: Any, name: String): Any? {
        val method = target.javaClass.methods.firstOrNull { it.name == name && it.parameterTypes.isEmpty() }
            ?: return null
        method.isAccessible = true
        return method.invoke(target)
    }
}
