package com.jamal2367.coreelec

import androidx.annotation.StringRes

/** How a line in the terminal window is marked and coloured. */
enum class LogKind { COMMAND, INFO, HINT, SUCCESS, WARNING, ERROR }

/** One line of the terminal window: either a string resource with [args] or a literal [text]. */
data class LogLine(
    val kind: LogKind,
    @param:StringRes val textRes: Int = 0,
    val args: List<Any> = emptyList(),
    val text: String? = null,
) {
    companion object {
        fun command(text: String) = LogLine(LogKind.COMMAND, text = text)
    }
}
