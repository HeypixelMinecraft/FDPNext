/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */
package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue

object ChatControl : Module("ChatControl", category = ModuleCategory.CLIENT, defaultOn = true) {
    val chatLimitValue = BoolValue("NoChatLimit", true)
    val chatClearValue = BoolValue("NoChatClear", true)
    val chatCombineValue = BoolValue("ChatCombine", true)
    val fontChatValue = BoolValue("FontChat", false)
    val chatRectValue = BoolValue("ChatBackGround", false)
    val betterChatRectValue = BoolValue("BetterChatRect", false)
    val chatAnimValue = BoolValue("ChatAnimation", false)
}