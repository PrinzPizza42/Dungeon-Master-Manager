package de.luca.dungeon_master_manager.data

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

object KeybindingAction {
    const val SEARCH_FILES = "search_files"
}

data class KeybindingDefinition(
    val id: String,
    val title: String,
    val description: String,
    val defaultShortcut: String
)

object KeybindingRegistry {
    val ALL: List<KeybindingDefinition> = listOf(
        KeybindingDefinition(
            id = KeybindingAction.SEARCH_FILES,
            title = "Open file search",
            description = "Opens the global file and quick search in the project",
            defaultShortcut = "Ctrl+F"
        )
    )

    fun defaultShortcuts(): Map<String, String> =
        ALL.associate { it.id to it.defaultShortcut }

    private val KEY_MAP: Map<Key, String> = mapOf(
        Key.A to "A", Key.B to "B", Key.C to "C", Key.D to "D", Key.E to "E",
        Key.F to "F", Key.G to "G", Key.H to "H", Key.I to "I", Key.J to "J",
        Key.K to "K", Key.L to "L", Key.M to "M", Key.N to "N", Key.O to "O",
        Key.P to "P", Key.Q to "Q", Key.R to "R", Key.S to "S", Key.T to "T",
        Key.U to "U", Key.V to "V", Key.W to "W", Key.X to "X", Key.Y to "Y",
        Key.Z to "Z",
        Key.Zero to "0", Key.One to "1", Key.Two to "2", Key.Three to "3",
        Key.Four to "4", Key.Five to "5", Key.Six to "6", Key.Seven to "7",
        Key.Eight to "8", Key.Nine to "9",
        Key.F1 to "F1", Key.F2 to "F2", Key.F3 to "F3", Key.F4 to "F4",
        Key.F5 to "F5", Key.F6 to "F6", Key.F7 to "F7", Key.F8 to "F8",
        Key.F9 to "F9", Key.F10 to "F10", Key.F11 to "F11", Key.F12 to "F12",
        Key.Spacebar to "Space", Key.Enter to "Enter", Key.Tab to "Tab",
        Key.Escape to "Esc", Key.Slash to "/", Key.Backslash to "\\"
    )

    private val NAME_TO_KEY: Map<String, Key> =
        KEY_MAP.entries.associate { (k, v) -> v.uppercase() to k }

    val MODIFIER_KEYS: Set<Key> = setOf(
        Key.CtrlLeft, Key.CtrlRight,
        Key.AltLeft, Key.AltRight,
        Key.ShiftLeft, Key.ShiftRight,
        Key.MetaLeft, Key.MetaRight
    )

    fun keyToName(key: Key): String? = KEY_MAP[key]

    fun nameToKey(name: String): Key? = NAME_TO_KEY[name.uppercase()]

    fun parseShortcutTokens(shortcut: String): List<String> {
        return shortcut.split("+")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Converts a KeyEvent to a shortcut string (e.g. "Ctrl+F", "Ctrl+Shift+P").
     * Returns null if only a modifier is pressed or key is unsupported.
     */
    fun keyEventToShortcut(event: KeyEvent): String? {
        if (event.type != KeyEventType.KeyDown) return null
        if (event.key in MODIFIER_KEYS) return null

        val keyName = keyToName(event.key) ?: return null

        val parts = mutableListOf<String>()
        if (event.isCtrlPressed || event.isMetaPressed) parts.add("Ctrl")
        if (event.isAltPressed) parts.add("Alt")
        if (event.isShiftPressed) parts.add("Shift")

        // Disallow bare single-character letters without modifier
        if (parts.isEmpty() && !keyName.startsWith("F")) {
            return null
        }

        parts.add(keyName)
        return parts.joinToString("+")
    }

    fun matchesShortcut(event: KeyEvent, shortcut: String): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        val tokens = parseShortcutTokens(shortcut)
        if (tokens.isEmpty()) return false

        val wantsCtrl = tokens.any { it.equals("Ctrl", true) || it.equals("Cmd", true) || it.equals("Meta", true) || it.equals("Strg", true) }
        val wantsAlt = tokens.any { it.equals("Alt", true) }
        val wantsShift = tokens.any { it.equals("Shift", true) }

        val hasCtrl = event.isCtrlPressed || event.isMetaPressed
        val hasAlt = event.isAltPressed
        val hasShift = event.isShiftPressed

        if (wantsCtrl != hasCtrl || wantsAlt != hasAlt || wantsShift != hasShift) return false

        val keyToken = tokens.lastOrNull { token ->
            !token.equals("Ctrl", true) &&
            !token.equals("Cmd", true) &&
            !token.equals("Meta", true) &&
            !token.equals("Strg", true) &&
            !token.equals("Alt", true) &&
            !token.equals("Shift", true)
        } ?: return false

        val expectedKey = nameToKey(keyToken) ?: return false
        return event.key == expectedKey
    }

    fun getAwtKeyCode(token: String): Int? {
        val upper = token.uppercase()
        if (upper.length == 1 && upper[0] in 'A'..'Z') {
            return java.awt.event.KeyEvent.VK_A + (upper[0] - 'A')
        }
        if (upper.length == 1 && upper[0] in '0'..'9') {
            return java.awt.event.KeyEvent.VK_0 + (upper[0] - '0')
        }
        return when (upper) {
            "F1" -> java.awt.event.KeyEvent.VK_F1
            "F2" -> java.awt.event.KeyEvent.VK_F2
            "F3" -> java.awt.event.KeyEvent.VK_F3
            "F4" -> java.awt.event.KeyEvent.VK_F4
            "F5" -> java.awt.event.KeyEvent.VK_F5
            "F6" -> java.awt.event.KeyEvent.VK_F6
            "F7" -> java.awt.event.KeyEvent.VK_F7
            "F8" -> java.awt.event.KeyEvent.VK_F8
            "F9" -> java.awt.event.KeyEvent.VK_F9
            "F10" -> java.awt.event.KeyEvent.VK_F10
            "F11" -> java.awt.event.KeyEvent.VK_F11
            "F12" -> java.awt.event.KeyEvent.VK_F12
            "SPACE" -> java.awt.event.KeyEvent.VK_SPACE
            "ENTER" -> java.awt.event.KeyEvent.VK_ENTER
            "TAB" -> java.awt.event.KeyEvent.VK_TAB
            "ESC", "ESCAPE" -> java.awt.event.KeyEvent.VK_ESCAPE
            else -> null
        }
    }

    fun matchesAwtEvent(event: java.awt.event.KeyEvent, shortcut: String): Boolean {
        if (event.id != java.awt.event.KeyEvent.KEY_PRESSED) return false
        val tokens = parseShortcutTokens(shortcut)
        if (tokens.isEmpty()) return false

        val wantsCtrl = tokens.any { it.equals("Ctrl", true) || it.equals("Cmd", true) || it.equals("Meta", true) || it.equals("Strg", true) }
        val wantsAlt = tokens.any { it.equals("Alt", true) }
        val wantsShift = tokens.any { it.equals("Shift", true) }

        val hasCtrl = event.isControlDown || event.isMetaDown
        val hasAlt = event.isAltDown
        val hasShift = event.isShiftDown

        if (wantsCtrl != hasCtrl || wantsAlt != hasAlt || wantsShift != hasShift) return false

        val keyToken = tokens.lastOrNull { token ->
            !token.equals("Ctrl", true) &&
            !token.equals("Cmd", true) &&
            !token.equals("Meta", true) &&
            !token.equals("Strg", true) &&
            !token.equals("Alt", true) &&
            !token.equals("Shift", true)
        } ?: return false

        val expectedCode = getAwtKeyCode(keyToken) ?: return false
        return event.keyCode == expectedCode
    }

    fun awtEventToShortcut(event: java.awt.event.KeyEvent): String? {
        if (event.id != java.awt.event.KeyEvent.KEY_PRESSED) return null
        if (event.keyCode in setOf(
            java.awt.event.KeyEvent.VK_CONTROL,
            java.awt.event.KeyEvent.VK_ALT,
            java.awt.event.KeyEvent.VK_SHIFT,
            java.awt.event.KeyEvent.VK_META
        )) return null

        val parts = mutableListOf<String>()
        if (event.isControlDown || event.isMetaDown) parts.add("Ctrl")
        if (event.isAltDown) parts.add("Alt")
        if (event.isShiftDown) parts.add("Shift")

        val keyName = when {
            event.keyCode in java.awt.event.KeyEvent.VK_A..java.awt.event.KeyEvent.VK_Z ->
                ('A' + (event.keyCode - java.awt.event.KeyEvent.VK_A)).toString()
            event.keyCode in java.awt.event.KeyEvent.VK_0..java.awt.event.KeyEvent.VK_9 ->
                ('0' + (event.keyCode - java.awt.event.KeyEvent.VK_0)).toString()
            event.keyCode in java.awt.event.KeyEvent.VK_F1..java.awt.event.KeyEvent.VK_F12 ->
                "F${event.keyCode - java.awt.event.KeyEvent.VK_F1 + 1}"
            event.keyCode == java.awt.event.KeyEvent.VK_SPACE -> "Space"
            event.keyCode == java.awt.event.KeyEvent.VK_ENTER -> "Enter"
            event.keyCode == java.awt.event.KeyEvent.VK_TAB -> "Tab"
            else -> null
        } ?: return null

        if (parts.isEmpty() && !keyName.startsWith("F")) return null
        parts.add(keyName)
        return parts.joinToString("+")
    }
}
