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

    var keyShortcutListener: KeyShortcutListener? = null

    var isKeyboardRestricted: Boolean = false
        set(value) {
            field = value
            applyKeyboardRestrictionJs(value)
        }

    fun applyKeyboardRestrictionJs(restricted: Boolean) {
        if (restricted) {
            evaluateJavascript("""
                (function() {
                    try {
                        var inputs = document.querySelectorAll('input, textarea');
                        for (var i = 0; i < inputs.length; i++) {
                            inputs[i].setAttribute('inputmode', 'none');
                        }
                    } catch(e) {}
                })();
            """.trimIndent(), null)
        } else {
            evaluateJavascript("""
                (function() {
                    try {
                        var inputs = document.querySelectorAll('input[inputmode="none"], textarea[inputmode="none"]');
                        for (var i = 0; i < inputs.length; i++) {
                            inputs[i].removeAttribute('inputmode');
                        }
                    } catch(e) {}
                })();
            """.trimIndent(), null)
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent?): Boolean {
        if (isKeyboardRestricted && event?.action == android.view.MotionEvent.ACTION_UP) {
            applyKeyboardRestrictionJs(true)
            post {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(windowToken, 0)
            }
        }
        return super.onTouchEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isCopy = event.keyCode == KeyEvent.KEYCODE_COPY || (event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_C)
        val isPaste = event.keyCode == KeyEvent.KEYCODE_PASTE || (event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_V)
        val isCut = event.keyCode == KeyEvent.KEYCODE_CUT || (event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_X)
        val isSelectAll = event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_A

        if (isCopy) {
            if (event.action == KeyEvent.ACTION_DOWN) copySelection()
            return true
        }
        if (isPaste) {
            if (event.action == KeyEvent.ACTION_DOWN) pasteClipboard()
            return true
        }
        if (isCut) {
            if (event.action == KeyEvent.ACTION_DOWN) cutSelection()
            return true
        }
        if (isSelectAll) {
            if (event.action == KeyEvent.ACTION_DOWN) selectAllContent()
            return true
        }

        if (event.action == KeyEvent.ACTION_DOWN && event.isCtrlPressed) {
            val handled = keyShortcutListener?.onShortcut(event.keyCode, event) ?: false
            if (handled) return true
        }
        return super.dispatchKeyEvent(event)
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

