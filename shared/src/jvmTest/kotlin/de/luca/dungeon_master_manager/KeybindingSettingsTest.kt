package de.luca.dungeon_master_manager

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import de.luca.dungeon_master_manager.data.KeybindingAction
import de.luca.dungeon_master_manager.data.KeybindingRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(InternalComposeUiApi::class)
class KeybindingSettingsTest {

    @Test
    fun testDefaultShortcuts() {
        val defaults = KeybindingRegistry.defaultShortcuts()
        assertEquals("Ctrl+F", defaults[KeybindingAction.SEARCH_FILES])
    }

    @Test
    fun testParseShortcutTokens() {
        val tokens = KeybindingRegistry.parseShortcutTokens("Ctrl + Shift + F")
        assertEquals(listOf("Ctrl", "Shift", "F"), tokens)
    }

    @Test
    fun testKeyNameMapping() {
        assertEquals("F", KeybindingRegistry.keyToName(Key.F))
        assertEquals(Key.F, KeybindingRegistry.nameToKey("F"))
        assertEquals(Key.P, KeybindingRegistry.nameToKey("p"))
        assertEquals(Key.F3, KeybindingRegistry.nameToKey("F3"))
    }

    @Test
    fun testMatchesShortcutCtrlF() {
        val event = KeyEvent(
            key = Key.F,
            type = KeyEventType.KeyDown,
            isCtrlPressed = true
        )

        assertTrue(KeybindingRegistry.matchesShortcut(event, "Ctrl+F"))
        assertTrue(KeybindingRegistry.matchesShortcut(event, "ctrl + f"))
        assertFalse(KeybindingRegistry.matchesShortcut(event, "Ctrl+P"))
        assertFalse(KeybindingRegistry.matchesShortcut(event, "Ctrl+Shift+F"))
        assertFalse(KeybindingRegistry.matchesShortcut(event, "Alt+F"))
    }

    @Test
    fun testMatchesShortcutCtrlShiftP() {
        val event = KeyEvent(
            key = Key.P,
            type = KeyEventType.KeyDown,
            isCtrlPressed = true,
            isShiftPressed = true
        )

        assertTrue(KeybindingRegistry.matchesShortcut(event, "Ctrl+Shift+P"))
        assertFalse(KeybindingRegistry.matchesShortcut(event, "Ctrl+P"))
        assertFalse(KeybindingRegistry.matchesShortcut(event, "Ctrl+F"))
    }

    @Test
    fun testKeyEventToShortcut() {
        val event = KeyEvent(
            key = Key.F,
            type = KeyEventType.KeyDown,
            isCtrlPressed = true
        )

        val shortcut = KeybindingRegistry.keyEventToShortcut(event)
        assertNotNull(shortcut)
        assertEquals("Ctrl+F", shortcut)
    }

    @Test
    fun testMatchesAwtEvent() {
        val dummy = object : java.awt.Component() {}
        val awtEvent = java.awt.event.KeyEvent(
            dummy,
            java.awt.event.KeyEvent.KEY_PRESSED,
            System.currentTimeMillis(),
            java.awt.event.InputEvent.CTRL_DOWN_MASK,
            java.awt.event.KeyEvent.VK_F,
            'f'
        )

        assertTrue(KeybindingRegistry.matchesAwtEvent(awtEvent, "Ctrl+F"))
        assertTrue(KeybindingRegistry.matchesAwtEvent(awtEvent, "Strg+F"))
        assertFalse(KeybindingRegistry.matchesAwtEvent(awtEvent, "Ctrl+P"))
        assertFalse(KeybindingRegistry.matchesAwtEvent(awtEvent, "Alt+F"))
    }

    @Test
    fun testAwtEventToShortcut() {
        val dummy = object : java.awt.Component() {}
        val awtEvent = java.awt.event.KeyEvent(
            dummy,
            java.awt.event.KeyEvent.KEY_PRESSED,
            System.currentTimeMillis(),
            java.awt.event.InputEvent.CTRL_DOWN_MASK,
            java.awt.event.KeyEvent.VK_F,
            'f'
        )

        val shortcut = KeybindingRegistry.awtEventToShortcut(awtEvent)
        assertEquals("Ctrl+F", shortcut)
    }
}
