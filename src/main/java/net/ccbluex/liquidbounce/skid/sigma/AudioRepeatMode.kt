package net.ccbluex.liquidbounce.skid.sigma

enum class AudioRepeatMode(val type: Int) {
    NO_REPEAT(0),
    REPEAT(1),
    LOOP_CURRENT(2);

    fun getNext(): AudioRepeatMode {
        for (mode in values()) {
            if (mode.type == this.type + 1) return mode
        }
        return NO_REPEAT
    }

    companion object {
        fun parse(type: Int): AudioRepeatMode {
            for (mode in values()) {
                if (mode.type == type) return mode
            }
            return REPEAT
        }
    }
}
