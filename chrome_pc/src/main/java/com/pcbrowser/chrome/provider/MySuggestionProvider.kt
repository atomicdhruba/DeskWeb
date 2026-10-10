package com.pcbrowser.chrome.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri

class MySuggestionProvider : ContentProvider() {

    private lateinit var dbHelper: DBHelper

    companion object {
        const val AUTHORITY = "com.pcbrowser.deskweb.provider.MySuggestionProvider"
        val URI_SEARCH: Uri = Uri.parse("content://$AUTHORITY/search_suggest_query")
        val URI_SUGGESTIONS: Uri = Uri.parse("content://$AUTHORITY/suggestions")

        private const val TABLE_SUGGESTIONS = "suggestions"
        private const val COL_ID = "_id"
        private const val COL_TERM = "term"
        private const val COL_LAST_USED = "last_used"

        private const val SEARCH_SUGGEST = 1
        private const val SUGGESTIONS = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "search_suggest_query", SEARCH_SUGGEST)
            addURI(AUTHORITY, "search_suggest_query/*", SEARCH_SUGGEST)
            addURI(AUTHORITY, TABLE_SUGGESTIONS, SUGGESTIONS)
        }
    }

    private class DBHelper(context: Context) : SQLiteOpenHelper(context, "suggestions.db", null, 2) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE suggestions (_id INTEGER PRIMARY KEY AUTOINCREMENT, term TEXT NOT NULL UNIQUE, last_used INTEGER NOT NULL);")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_suggestions_term ON suggestions(term);")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_suggestions_last_used ON suggestions(last_used);")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS suggestions")
            onCreate(db)
        }
    }

    override fun onCreate(): Boolean {
        context?.let { dbHelper = DBHelper(it) }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val queryText = if (!selectionArgs.isNullOrEmpty()) {
            selectionArgs[0]
        } else if (uri.pathSegments.size > 1) {
            uri.lastPathSegment
        } else {
            ""
        } ?: ""

        val db = dbHelper.readableDatabase
        val columns = arrayOf("_id", "suggest_text_1")
        val matrixCursor = MatrixCursor(columns)

        if (queryText.isEmpty()) {
            val cursor = db.query(
                TABLE_SUGGESTIONS,
                arrayOf(COL_ID, COL_TERM),
                null,
                null,
                null,
                null,
                "$COL_LAST_USED DESC",
                "10"
            )
            cursor.use {
                while (it.moveToNext()) {
                    matrixCursor.addRow(arrayOf(it.getLong(0), it.getString(1)))
                }
            }
        } else {
            val cursor = db.query(
                TABLE_SUGGESTIONS,
                arrayOf(COL_ID, COL_TERM),
                "$COL_TERM LIKE ?",
                arrayOf("$queryText%"),
                null,
                null,
                "$COL_LAST_USED DESC",
                "10"
            )
            cursor.use {
                while (it.moveToNext()) {
                    matrixCursor.addRow(arrayOf(it.getLong(0), it.getString(1)))
                }
            }
        }
        return matrixCursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (values == null || !values.containsKey(COL_TERM)) return null
        val db = dbHelper.writableDatabase
        if (!values.containsKey(COL_LAST_USED)) {
            values.put(COL_LAST_USED, System.currentTimeMillis())
        }
        val id = db.insertWithOnConflict(TABLE_SUGGESTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        context?.contentResolver?.notifyChange(URI_SEARCH, null)
        return ContentUris.withAppendedId(uri, id)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val count = dbHelper.writableDatabase.delete(TABLE_SUGGESTIONS, selection, selectionArgs)
        context?.contentResolver?.notifyChange(URI_SEARCH, null)
        return count
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.com.pcbrowser.deskweb.suggestion"
}
