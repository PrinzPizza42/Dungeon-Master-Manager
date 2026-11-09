package de.luca.dungeon_master_manager

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import de.luca.dungeon_master_manager.data.KeybindingAction
import de.luca.dungeon_master_manager.data.KeybindingRegistry
import de.luca.dungeon_master_manager.data.KeybindingScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(InternalComposeUiApi::class)
class KeybindingSettingsTest {

    @Test
    fun testDefaultShortcuts() {
        val defaults = KeybindingRegistry.defaultShortcuts()
        assertEquals("Ctrl+F", defaults[KeybindingAction.SEARCH_FILES])
        assertEquals("Ctrl+B", defaults[KeybindingAction.TOGGLE_EXPLORER])
        assertEquals("Ctrl+W", defaults[KeybindingAction.CLOSE_TAB])
        assertEquals("Ctrl+Tab", defaults[KeybindingAction.NEXT_TAB])
        assertEquals("Ctrl+Shift+Tab", defaults[KeybindingAction.PREV_TAB])
        assertEquals("Ctrl+,", defaults[KeybindingAction.OPEN_SETTINGS])
        assertEquals("Ctrl+E", defaults[KeybindingAction.OPEN_ENTITIES])
        assertEquals("Ctrl+N", defaults[KeybindingAction.NEW_FILE])
        assertEquals("Ctrl+S", defaults[KeybindingAction.SAVE_FILE])
        assertEquals("Ctrl+B", defaults[KeybindingAction.FORMAT_BOLD])
        assertEquals("Ctrl+I", defaults[KeybindingAction.FORMAT_ITALIC])
    }

    @Test
    fun testParseShortcutTokens() {
        val tokens = KeybindingRegistry.parseShortcutTokens("Ctrl + Shift + F")
        assertEquals(listOf("Ctrl", "Shift", "F"), tokens)

        val commaTokens = KeybindingRegistry.parseShortcutTokens("Ctrl+,")
        assertEquals(listOf("Ctrl", ","), commaTokens)

        val emptyTokens = KeybindingRegistry.parseShortcutTokens("")
        assertTrue(emptyTokens.isEmpty())
    }

    @Test
    fun testNormalizeShortcut() {
        assertEquals("Ctrl+F", KeybindingRegistry.normalizeShortcut("ctrl + f"))
        assertEquals("Ctrl+Shift+TAB", KeybindingRegistry.normalizeShortcut("shift + ctrl + tab"))
        assertEquals("Ctrl+,", KeybindingRegistry.normalizeShortcut("ctrl+,"))
        assertEquals("", KeybindingRegistry.normalizeShortcut(""))
    }

    @Test
    fun testKeyNameMapping() {
        assertEquals("F", KeybindingRegistry.keyToName(Key.F))
        assertEquals(Key.F, KeybindingRegistry.nameToKey("F"))
        assertEquals(Key.P, KeybindingRegistry.nameToKey("p"))
        assertEquals(Key.F3, KeybindingRegistry.nameToKey("F3"))
        assertEquals(Key.Comma, KeybindingRegistry.nameToKey(","))
        assertEquals(Key.Tab, KeybindingRegistry.nameToKey("Tab"))
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
    fun testMatchesShortcutCtrlComma() {
        val event = KeyEvent(
            key = Key.Comma,
            type = KeyEventType.KeyDown,
            isCtrlPressed = true
        )

        assertTrue(KeybindingRegistry.matchesShortcut(event, "Ctrl+,"))
        assertFalse(KeybindingRegistry.matchesShortcut(event, "Ctrl+."))
    }

    @Test
    fun testMatchesShortcutCtrlTab() {
        val event = KeyEvent(
            key = Key.Tab,
            type = KeyEventType.KeyDown,
            isCtrlPressed = true
        )

        assertTrue(KeybindingRegistry.matchesShortcut(event, "Ctrl+Tab"))
        assertFalse(KeybindingRegistry.matchesShortcut(event, "Ctrl+Shift+Tab"))
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

        val commaEvent = java.awt.event.KeyEvent(
            dummy,
            java.awt.event.KeyEvent.KEY_PRESSED,
            System.currentTimeMillis(),
            java.awt.event.InputEvent.CTRL_DOWN_MASK,
            java.awt.event.KeyEvent.VK_COMMA,
            ','
        )
        assertTrue(KeybindingRegistry.matchesAwtEvent(commaEvent, "Ctrl+,"))
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

        val commaEvent = java.awt.event.KeyEvent(
            dummy,
            java.awt.event.KeyEvent.KEY_PRESSED,
            System.currentTimeMillis(),
            java.awt.event.InputEvent.CTRL_DOWN_MASK,
            java.awt.event.KeyEvent.VK_COMMA,
            ','
        )
        assertEquals("Ctrl+,", KeybindingRegistry.awtEventToShortcut(commaEvent))
    }

    @Test
    fun testConflictDetectionWithinSameScope() {
        val current = KeybindingRegistry.defaultShortcuts()

        // Trying to set CLOSE_TAB (Global) to "Ctrl+B" conflicts with TOGGLE_EXPLORER (Global)
        val conflict = KeybindingRegistry.findConflict(
            actionId = KeybindingAction.CLOSE_TAB,
            newShortcut = "Ctrl+B",
            currentShortcuts = current
        )
        assertNotNull(conflict)
        assertEquals(KeybindingAction.TOGGLE_EXPLORER, conflict.id)
        assertEquals(KeybindingScope.GLOBAL, conflict.scope)
    }

    @Test
    fun testNoConflictAcrossDifferentScopes() {
        val current = KeybindingRegistry.defaultShortcuts()

        // Both TOGGLE_EXPLORER (Global) and FORMAT_BOLD (Editor) default to "Ctrl+B"
        // Because they have different scopes, FORMAT_BOLD does not conflict with TOGGLE_EXPLORER
        val conflict = KeybindingRegistry.findConflict(
            actionId = KeybindingAction.FORMAT_BOLD,
            newShortcut = "Ctrl+B",
            currentShortcuts = current
        )
        assertNull(conflict, "Same shortcut in different scopes must NOT conflict")
    }

    @Test
    fun testConflictWithinEditorScope() {
        val current = KeybindingRegistry.defaultShortcuts()

        // Setting FORMAT_ITALIC (Editor) to "Ctrl+B" conflicts with FORMAT_BOLD (Editor)
        val conflict = KeybindingRegistry.findConflict(
            actionId = KeybindingAction.FORMAT_ITALIC,
            newShortcut = "Ctrl+B",
            currentShortcuts = current
        )
        assertNotNull(conflict)
        assertEquals(KeybindingAction.FORMAT_BOLD, conflict.id)
        assertEquals(KeybindingScope.EDITOR, conflict.scope)
    }

    @Test
    fun testUnboundShortcutNeverConflicts() {
        val current = KeybindingRegistry.defaultShortcuts()

        val conflict = KeybindingRegistry.findConflict(
            actionId = KeybindingAction.CLOSE_TAB,
            newShortcut = "",
            currentShortcuts = current
        )
        assertNull(conflict)
    }
}
