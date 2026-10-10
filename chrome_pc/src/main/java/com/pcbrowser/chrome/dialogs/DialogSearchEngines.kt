package com.pcbrowser.chrome.dialogs

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import com.pcbrowser.chrome.R
import com.pcbrowser.chrome.data.SettingsPreferences
import com.pcbrowser.chrome.search.SearchEngineManager

class DialogSearchEngines(
    private val context: Context,
    private val settings: SettingsPreferences,
    private val onEngineSelected: (String) -> Unit
) {

    fun show() {
        val engines = SearchEngineManager.ENGINES
        val names = engines.map { it.name }.toTypedArray()
        val currentId = settings.activeSearchEngineId
        val currentIndex = engines.indexOfFirst { it.id.equals(currentId, ignoreCase = true) }.let {
            if (it >= 0) it else 0
        }

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.search_engine_title))
            .setSingleChoiceItems(names, currentIndex) { dialog: DialogInterface, which: Int ->
                val selected = engines[which]
                settings.activeSearchEngineId = selected.id
                onEngineSelected(selected.id)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
