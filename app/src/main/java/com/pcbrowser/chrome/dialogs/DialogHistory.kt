package com.pcbrowser.chrome.dialogs

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.pcbrowser.chrome.R
import com.pcbrowser.chrome.history.HistoryManager
import com.pcbrowser.chrome.history.HistoryModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DialogHistory(
    private val context: Context,
    private val historyManager: HistoryManager,
    private val activeProfileId: String,
    private val onUrlSelected: (String) -> Unit
) {

    private lateinit var dialog: AlertDialog
    private lateinit var listContainer: LinearLayout
    private lateinit var tvEmpty: TextView
    private val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.app_browser_dialog_history, null)
        val etSearch = view.findViewById<EditText>(R.id.etHistorySearch)
        val btnClear = view.findViewById<Button>(R.id.btnClearHistory)
        val btnClose = view.findViewById<Button>(R.id.btnCloseHistory)
        listContainer = view.findViewById(R.id.historyListContainer)
        tvEmpty = view.findViewById(R.id.tvHistoryEmpty)

        dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        renderList("")

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                renderList(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClear.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear all browsing history?")
                .setPositiveButton("Clear") { _, _ ->
                    historyManager.clearHistory(activeProfileId)
                    renderList(etSearch.text.toString())
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun renderList(query: String) {
        listContainer.removeAllViews()
        val all = historyManager.getHistory(activeProfileId)
        val filtered = if (query.isBlank()) {
            all
        } else {
            all.filter {
                it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
            }
        }

        if (filtered.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            return
        }
        tvEmpty.visibility = View.GONE

        val inflater = LayoutInflater.from(context)
        for (item in filtered) {
            val itemView = inflater.inflate(R.layout.app_browser_history_item, listContainer, false)
            val tvTitle = itemView.findViewById<TextView>(R.id.tvHistoryTitle)
            val tvUrl = itemView.findViewById<TextView>(R.id.tvHistoryUrl)
            val tvTime = itemView.findViewById<TextView>(R.id.tvHistoryTime)
            val btnDelete = itemView.findViewById<ImageButton>(R.id.btnDeleteHistoryItem)

            tvTitle.text = item.title
            tvUrl.text = item.url
            tvTime.text = dateFormat.format(Date(item.timestamp))

            itemView.setOnClickListener {
                dialog.dismiss()
                onUrlSelected(item.url)
            }

            btnDelete.setOnClickListener {
                historyManager.deleteEntry(activeProfileId, item.id)
                renderList(query)
            }

            listContainer.addView(itemView)
        }
    }
}
