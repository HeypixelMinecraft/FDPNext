/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.script.remapper.injection.utils

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList

/**
 * A bytecode node util
 *
 * @author CCBlueX
 */
object NodeUtils {

    /**
     * Lazy.
     */
    fun toNodes(vararg nodes: AbstractInsnNode): InsnList {
        val insnList = InsnList()
        for (node in nodes)
            insnList.add(node)
        return insnList
    }

}