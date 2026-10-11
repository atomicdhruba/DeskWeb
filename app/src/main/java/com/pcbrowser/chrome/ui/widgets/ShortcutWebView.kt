package com.pcbrowser.chrome.ui.widgets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.webkit.WebView
import org.json.JSONObject
import org.json.JSONTokener

class ShortcutWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    interface KeyShortcutListener {
        fun onShortcut(keyCode: Int, event: KeyEvent): Boolean
    }

    interface MouseEventListener {
        fun onMiddleClick(url: String?)
        fun onZoomIn()
        fun onZoomOut()
        fun onNavigateBack()
        fun onNavigateForward()
        fun onShowContextMenu(x: Float, y: Float)
    }

    var keyShortcutListener: KeyShortcutListener? = null
    var mouseEventListener: MouseEventListener? = null

    var isKeyboardRestricted: Boolean = false
        set(value) {
            field = value
            applyKeyboardRestrictionJs(value)
        }

    fun applyKeyboardRestrictionJs(restricted: Boolean) {
        val script = if (restricted) {
            """
                (function() {
                    try {
                        if (!window.__deskwebKeyboardObserver) {
                            window.__deskwebKeyboardObserver = new MutationObserver(function() {
                                if (window.__deskwebRestricted) {
                                    var items = document.querySelectorAll('input:not([inputmode="none"]), textarea:not([inputmode="none"])');
                                    for (var i = 0; i < items.length; i++) {
                                        items[i].setAttribute('inputmode', 'none');
                                    }
                                }
                            });
                        }
                        window.__deskwebRestricted = true;
                        var items = document.querySelectorAll('input, textarea');
                        for (var i = 0; i < items.length; i++) {
                            items[i].setAttribute('inputmode', 'none');
                        }
                        window.__deskwebKeyboardObserver.observe(document.documentElement || document.body, { childList: true, subtree: true });
                    } catch(e) {}
                })();
            """.trimIndent()
        } else {
            """
                (function() {
                    try {
                        window.__deskwebRestricted = false;
                        if (window.__deskwebKeyboardObserver) {
                            window.__deskwebKeyboardObserver.disconnect();
                        }
                        var items = document.querySelectorAll('input[inputmode="none"], textarea[inputmode="none"]');
                        for (var i = 0; i < items.length; i++) {
                            items[i].removeAttribute('inputmode');
                        }
                    } catch(e) {}
                })();
            """.trimIndent()
        }
        evaluateJavascript(script, null)
    }

    override fun onGenericMotionEvent(event: android.view.MotionEvent): Boolean {
        // 1. Mouse Scroll Wheel interactions
        if (event.action == android.view.MotionEvent.ACTION_SCROLL) {
            val vScroll = event.getAxisValue(android.view.MotionEvent.AXIS_VSCROLL)
            val isCtrl = (event.metaState and KeyEvent.META_CTRL_ON) != 0
            val isShift = (event.metaState and KeyEvent.META_SHIFT_ON) != 0
            if (isCtrl) {
                // Ctrl + Mouse Wheel Zoom
                if (vScroll > 0) {
                    mouseEventListener?.onZoomIn()
                    return true
                } else if (vScroll < 0) {
                    mouseEventListener?.onZoomOut()
                    return true
                }
            } else if (isShift && vScroll != 0f) {
                // Shift + Mouse Wheel Horizontal Scroll
                scrollBy((-vScroll * 60).toInt(), 0)
                return true
            }
        }

        // 2. Mouse Button Press (Thumb Buttons & Middle Click)
        if (event.action == android.view.MotionEvent.ACTION_BUTTON_PRESS) {
            if ((event.buttonState and android.view.MotionEvent.BUTTON_BACK) != 0) {
                mouseEventListener?.onNavigateBack()
                return true
            }
            if ((event.buttonState and android.view.MotionEvent.BUTTON_FORWARD) != 0) {
                mouseEventListener?.onNavigateForward()
                return true
            }
            if ((event.buttonState and android.view.MotionEvent.BUTTON_TERTIARY) != 0) {
                handleMiddleClick(event.x, event.y)
                return true
            }
            if ((event.buttonState and android.view.MotionEvent.BUTTON_SECONDARY) != 0) {
                mouseEventListener?.onShowContextMenu(event.x, event.y)
                return true
            }
        }

        return super.onGenericMotionEvent(event)
    }

    override fun onTouchEvent(event: android.view.MotionEvent?): Boolean {
        if (event != null) {
            // Middle-Click (wheel click) detection
            if (event.action == android.view.MotionEvent.ACTION_DOWN &&
                (event.buttonState and android.view.MotionEvent.BUTTON_TERTIARY) != 0
            ) {
                handleMiddleClick(event.x, event.y)
                return true
            }

            // Right-Click (secondary button) detection
            if (event.action == android.view.MotionEvent.ACTION_DOWN &&
                (event.buttonState and android.view.MotionEvent.BUTTON_SECONDARY) != 0
            ) {
                mouseEventListener?.onShowContextMenu(event.x, event.y)
                return true
            }
        }

        if (isKeyboardRestricted && event?.action == android.view.MotionEvent.ACTION_UP) {
            applyKeyboardRestrictionJs(true)
            post {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(windowToken, 0)
            }
        }
        return super.onTouchEvent(event)
    }

    fun handleMiddleClick(x: Float, y: Float) {
        val result = hitTestResult
        val linkUrl = if (result.type == HitTestResult.SRC_ANCHOR_TYPE || result.type == HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            result.extra
        } else null

        if (!linkUrl.isNullOrEmpty()) {
            mouseEventListener?.onMiddleClick(linkUrl)
        } else {
            val density = resources.displayMetrics.density
            val cssX = (x / density).toInt()
            val cssY = (y / density).toInt()
            evaluateJavascript("""
                (function() {
                    var el = document.elementFromPoint($cssX, $cssY);
                    var a = el ? el.closest('a') : null;
                    return a ? a.href : '';
                })()
            """.trimIndent()) { res ->
                val parsed = res?.trim('"')?.replace("\\\"", "\"")
                if (!parsed.isNullOrEmpty() && parsed != "null" && parsed != "\"\"") {
                    post { mouseEventListener?.onMiddleClick(parsed) }
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // 1. Let parent handle browser navigation shortcuts (Ctrl+T, Ctrl+W, Ctrl+R, F5, F11, etc.)
        if (event.action == KeyEvent.ACTION_DOWN) {
            val handled = keyShortcutListener?.onShortcut(event.keyCode, event) ?: false
            if (handled) return true
        }

        // 2. Delegate to Chromium's native keyboard pipeline (typing, arrows, forms)
        val chromiumHandled = super.dispatchKeyEvent(event)
        if (chromiumHandled) return true

        // 3. Fallback editing shortcuts if not consumed by page
        if (event.action == KeyEvent.ACTION_DOWN) {
            val isCopy = event.keyCode == KeyEvent.KEYCODE_COPY || (event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_C)
            val isPaste = event.keyCode == KeyEvent.KEYCODE_PASTE || (event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_V)
            val isCut = event.keyCode == KeyEvent.KEYCODE_CUT || (event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_X)
            val isSelectAll = event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_A
            val isUndo = event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_Z && !event.isShiftPressed
            val isRedo = (event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_Y) || (event.isCtrlPressed && event.isShiftPressed && event.keyCode == KeyEvent.KEYCODE_Z)

            when {
                isCopy -> {
                    copySelection()
                    return true
                }
                isPaste -> {
                    pasteClipboard()
                    return true
                }
                isCut -> {
                    cutSelection()
                    return true
                }
                isSelectAll -> {
                    selectAllContent()
                    return true
                }
                isUndo -> {
                    evaluateJavascript("document.execCommand('undo')", null)
                    return true
                }
                isRedo -> {
                    evaluateJavascript("document.execCommand('redo')", null)
                    return true
                }
            }
        }

        return false
    }

    fun copySelection() {
        // Execute document copy
        evaluateJavascript("document.execCommand('copy')", null)

        // Also retrieve selection to ensure Android system clipboard receives it
        val jsGetSelection = """
            (function() {
                var s = window.getSelection();
                if (s && s.toString().length > 0) return s.toString();
                var a = document.activeElement;
                if (a && (a.tagName === 'INPUT' || a.tagName === 'TEXTAREA')) {
                    var st = a.selectionStart;
                    var en = a.selectionEnd;
                    if (st !== undefined && en !== undefined && st !== en) {
                        return a.value.substring(st, en);
                    }
                }
                return '';
            })()
        """.trimIndent()

        evaluateJavascript(jsGetSelection) { result ->
            if (!result.isNullOrBlank() && result != "\"\"" && result != "null") {
                try {
                    val parsed = JSONTokener(result).nextValue()?.toString() ?: ""
                    if (parsed.isNotEmpty()) {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("DeskWeb", parsed))
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun pasteClipboard() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = cm?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val pasteContent = clip.getItemAt(0).coerceToText(context).toString()
            if (pasteContent.isNotEmpty()) {
                val escaped = JSONObject.quote(pasteContent)
                val jsPaste = """
                    (function() {
                        var active = document.activeElement;
                        var inserted = false;
                        try {
                            inserted = document.execCommand('insertText', false, $escaped);
                        } catch(e) {}
                        if (!inserted && active && (active.tagName === 'INPUT' || active.tagName === 'TEXTAREA')) {
                            var start = active.selectionStart !== undefined ? active.selectionStart : active.value.length;
                            var end = active.selectionEnd !== undefined ? active.selectionEnd : active.value.length;
                            var val = active.value || '';
                            active.value = val.substring(0, start) + $escaped + val.substring(end);
                            active.selectionStart = active.selectionEnd = start + $escaped.length;
                            active.dispatchEvent(new Event('input', { bubbles: true }));
                            active.dispatchEvent(new Event('change', { bubbles: true }));
                        }
                    })()
                """.trimIndent()
                evaluateJavascript(jsPaste, null)
            }
        }
    }

    fun cutSelection() {
        copySelection()
        evaluateJavascript("""
            (function() {
                var active = document.activeElement;
                var cut = false;
                try {
                    cut = document.execCommand('cut');
                } catch(e) {}
                if (!cut && active && (active.tagName === 'INPUT' || active.tagName === 'TEXTAREA')) {
                    var start = active.selectionStart;
                    var end = active.selectionEnd;
                    if (start !== undefined && end !== undefined && start !== end) {
                        var val = active.value || '';
                        active.value = val.substring(0, start) + val.substring(end);
                        active.selectionStart = active.selectionEnd = start;
                        active.dispatchEvent(new Event('input', { bubbles: true }));
                        active.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                }
            })()
        """.trimIndent(), null)
    }

    fun selectAllContent() {
        evaluateJavascript("""
            (function() {
                var active = document.activeElement;
                if (active && (active.tagName === 'INPUT' || active.tagName === 'TEXTAREA')) {
                    active.select();
                } else {
                    document.execCommand('selectAll');
                }
            })()
        """.trimIndent(), null)
    }
}

