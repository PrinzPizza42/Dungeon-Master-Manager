package de.luca.dungeon_master_manager.ui.components

import com.sun.jna.CallbackReference
import com.sun.jna.Native
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import java.awt.Window
import java.util.concurrent.ConcurrentHashMap

/**
 * Native Windows DWM integration helper using JNA.
 *
 * Provides:
 * - DWM window procedure subclassing (WM_NCCALCSIZE) to allow full-bleed Compose content
 *   while retaining native Windows window frame styles (WS_CAPTION, WS_THICKFRAME).
 * - Hardware-accelerated window animations by dispatching native WM_SYSCOMMAND
 *   messages (SC_MINIMIZE, SC_MAXIMIZE, SC_RESTORE, SC_CLOSE).
 * - Safe fallback on non-Windows platforms.
 */
object WindowsDwmHelper {

    val isWindows: Boolean = Platform.isWindows()

    private const val WM_NCCALCSIZE = 0x0083
    private const val WM_SYSCOMMAND = 0x0112
    private const val SC_MINIMIZE = 0xF020
    private const val SC_MAXIMIZE = 0xF030
    private const val SC_RESTORE = 0xF120
    private const val SC_CLOSE = 0xF060

    private const val GWLP_WNDPROC = -4

    // Keep strong references to prevent GC of native callbacks
    private val installedProcs = ConcurrentHashMap<Long, WinUser.WindowProc>()
    private val oldProcs = ConcurrentHashMap<Long, Pointer>()

    fun getHwnd(window: Window): WinDef.HWND? {
        if (!isWindows) return null
        return try {
            val ptr = Native.getComponentPointer(window) ?: return null
            WinDef.HWND(ptr)
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Subclasses the window procedure to handle WM_NCCALCSIZE so the client area
     * extends across the entire window (removing the standard Windows caption bar)
     * while retaining WS_CAPTION and WS_THICKFRAME for native DWM animations.
     */
    fun install(window: Window) {
        if (!isWindows) return
        try {
            val hwnd = getHwnd(window) ?: return
            val key = Pointer.nativeValue(hwnd.pointer)
            if (installedProcs.containsKey(key)) return

            // Ensure window has standard DWM frame styles so DWM treats it as an animated frame
            val currentStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_STYLE)
            val dwmStyle = currentStyle or WinUser.WS_CAPTION or WinUser.WS_THICKFRAME or WinUser.WS_MINIMIZEBOX or WinUser.WS_MAXIMIZEBOX
            User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_STYLE, dwmStyle)

            val wndProc = object : WinUser.WindowProc {
                override fun callback(
                    hWnd: WinDef.HWND?,
                    uMsg: Int,
                    wParam: WinDef.WPARAM?,
                    lParam: WinDef.LPARAM?
                ): WinDef.LRESULT {
                    val oldProc = oldProcs[key]

                    if (uMsg == WM_NCCALCSIZE) {
                        // Returning 0 indicates that the client area occupies the entire window rectangle
                        return WinDef.LRESULT(0)
                    }

                    return if (oldProc != null) {
                        User32.INSTANCE.CallWindowProc(oldProc, hWnd, uMsg, wParam, lParam)
                    } else {
                        User32.INSTANCE.DefWindowProc(hWnd, uMsg, wParam, lParam)
                    }
                }
            }

            installedProcs[key] = wndProc
            val procPointer = CallbackReference.getFunctionPointer(wndProc)

            val old = if (Platform.is64Bit()) {
                User32.INSTANCE.SetWindowLongPtr(hwnd, GWLP_WNDPROC, procPointer)
            } else {
                val ptrVal = User32.INSTANCE.SetWindowLong(hwnd, GWLP_WNDPROC, Pointer.nativeValue(procPointer).toInt())
                Pointer(ptrVal.toLong())
            }
            if (old != null) {
                oldProcs[key] = old
            }

            // Force recalculation of frame
            User32.INSTANCE.SetWindowPos(
                hwnd,
                null,
                0, 0, 0, 0,
                WinUser.SWP_NOMOVE or WinUser.SWP_NOSIZE or WinUser.SWP_NOZORDER or WinUser.SWP_FRAMECHANGED
            )
        } catch (e: Throwable) {
            // Silently fallback if JNA native hook fails on this environment
        }
    }

    /**
     * Sends SC_MINIMIZE via WM_SYSCOMMAND to trigger the native taskbar fly-down animation.
     */
    fun minimize(window: Window): Boolean {
        if (!isWindows) return false
        val hwnd = getHwnd(window) ?: return false
        return try {
            User32.INSTANCE.SendMessage(hwnd, WM_SYSCOMMAND, WinDef.WPARAM(SC_MINIMIZE.toLong()), WinDef.LPARAM(0))
            true
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Sends SC_MAXIMIZE via WM_SYSCOMMAND to trigger the native zoom animation.
     */
    fun maximize(window: Window): Boolean {
        if (!isWindows) return false
        val hwnd = getHwnd(window) ?: return false
        return try {
            User32.INSTANCE.SendMessage(hwnd, WM_SYSCOMMAND, WinDef.WPARAM(SC_MAXIMIZE.toLong()), WinDef.LPARAM(0))
            true
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Sends SC_RESTORE via WM_SYSCOMMAND to trigger the native restore animation.
     */
    fun restore(window: Window): Boolean {
        if (!isWindows) return false
        val hwnd = getHwnd(window) ?: return false
        return try {
            User32.INSTANCE.SendMessage(hwnd, WM_SYSCOMMAND, WinDef.WPARAM(SC_RESTORE.toLong()), WinDef.LPARAM(0))
            true
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Toggles between maximize and restore with native animations.
     */
    fun maximizeOrRestore(window: Window, isMaximized: Boolean): Boolean {
        return if (isMaximized) restore(window) else maximize(window)
    }

    /**
     * Brings the native Windows window to top and sets it as the foreground window.
     */
    fun activate(window: Window): Boolean {
        if (!isWindows) return false
        val hwnd = getHwnd(window) ?: return false
        return try {
            User32.INSTANCE.BringWindowToTop(hwnd)
            User32.INSTANCE.SetForegroundWindow(hwnd)
            true
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Sends SC_CLOSE via WM_SYSCOMMAND to trigger standard window closing.
     */
    fun close(window: Window): Boolean {
        if (!isWindows) return false
        val hwnd = getHwnd(window) ?: return false
        return try {
            User32.INSTANCE.SendMessage(hwnd, WM_SYSCOMMAND, WinDef.WPARAM(SC_CLOSE.toLong()), WinDef.LPARAM(0))
            true
        } catch (e: Throwable) {
            false
        }
    }
}
