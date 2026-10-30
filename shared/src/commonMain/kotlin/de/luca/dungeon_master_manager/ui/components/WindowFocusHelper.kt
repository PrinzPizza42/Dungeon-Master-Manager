package de.luca.dungeon_master_manager.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import java.awt.Frame
import java.awt.Window
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * Robustly brings an AWT/Swing/Compose window to the front and requests input focus.
 *
 * Uses:
 * 1. Restoring from ICONIFIED (minimized) state if needed.
 * 2. Native Win32 SetForegroundWindow / BringWindowToTop on Windows via WindowsDwmHelper.
 * 3. Cross-platform isAlwaysOnTop toggle trick to force OS window managers
 *    (both X11/Wayland with Focus Stealing Prevention and Windows) to elevate the window
 *    layer, with a brief timer to revert isAlwaysOnTop so the window is not permanently pinned.
 */
fun Window.bringToFrontAndFocus() {
    SwingUtilities.invokeLater {
        try {
            if (this is Frame && (state and Frame.ICONIFIED) != 0) {
                state = state and Frame.ICONIFIED.inv()
            }

            val activatedNatively = if (WindowsDwmHelper.isWindows) {
                WindowsDwmHelper.activate(this)
            } else false

            toFront()
            requestFocus()

            // On Linux/X11 or if native activation failed, use the isAlwaysOnTop elevation trick
            if (!activatedNatively) {
                val wasAlwaysOnTop = isAlwaysOnTop
                if (!wasAlwaysOnTop) {
                    isAlwaysOnTop = true
                }
                if (!wasAlwaysOnTop) {
                    // Keep always-on-top briefly (150ms) so the OS window manager processes the Z-order elevation,
                    // then release it so normal window stacking behavior resumes.
                    val timer = Timer(150) {
                        try {
                            isAlwaysOnTop = false
                        } catch (_: Throwable) {}
                    }
                    timer.isRepeats = false
                    timer.start()
                }
            }
        } catch (_: Throwable) {
            try {
                toFront()
                requestFocus()
            } catch (_: Throwable) {}
        }
    }
}

/**
 * Composable effect that ensures a window is brought to the front and focused:
 * 1. As soon as the window is displayed (via ComponentListener.componentShown).
 * 2. Immediately if already showing.
 * 3. Whenever [trigger] changes (e.g. user requested opening an already-open window).
 */
@Composable
fun BringWindowToFrontEffect(window: Window, trigger: Any? = null) {
    DisposableEffect(window, trigger) {
        val listener = object : java.awt.event.ComponentAdapter() {
            override fun componentShown(e: java.awt.event.ComponentEvent?) {
                window.bringToFrontAndFocus()
            }
        }
        window.addComponentListener(listener)

        if (window.isShowing) {
            window.bringToFrontAndFocus()
        }

        onDispose {
            window.removeComponentListener(listener)
        }
    }
}
