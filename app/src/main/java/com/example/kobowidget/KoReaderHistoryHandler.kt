package com.example.kobowidget

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.github.luben.zstd.Zstd
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class KoReaderHistoryHandler (
    private var context: Context,
    historyUri: Uri
) {
    private lateinit var bookHistory: MutableList<BookFile>

    init {
        bookHistory = mutableListOf<BookFile>()
        readBookHistory(historyUri)
    }

    fun getCurrentReadingBook(): BookFile? {
//        return bookHistory.maxByOrNull { it.time }
        if (bookHistory.size > 0) return bookHistory[0]
        return null
    }

    private fun readBookHistory(historyUri: Uri) {
        try {
            // 使用 ContentResolver 打开 URI 并读取内容
            val fileContent = context.contentResolver.openInputStream(historyUri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalArgumentException("Cannot open URI: $historyUri")

            parseLuaTable(fileContent)

        } catch (e: Exception) {
            e.printStackTrace()
            println("Error reading book history: ${e.message}")
        }
    }

    private fun parseLuaTable(input: String) {

        val entryRegex = "\\[(\\d+)\\] = \\{(.*?)\\}".toRegex(RegexOption.DOT_MATCHES_ALL)
        val fileRegex = "\\[\"file\"\\] = \"(.*?)\"".toRegex()
        val timeRegex = "\\[\"time\"\\] = (\\d+)".toRegex()

        entryRegex.findAll(input).forEach { matchResult ->
            val entryContent = matchResult.groupValues[2]

            val fileMatch = fileRegex.find(entryContent)
            val timeMatch = timeRegex.find(entryContent)

            if (fileMatch != null && timeMatch != null) {
                val file = fileMatch.groupValues[1]
                val time = timeMatch.groupValues[1].toLong()
                bookHistory.add(BookFile(file, time))
            }
        }
    }

}

data class BookFile(
    val filename: String,
    val time: Long
)