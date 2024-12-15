package com.example.kobowidget

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class KoReadingStatisticsDBHandler (
    context: Context,
    dbPath: Uri
) {
    private lateinit var context: Context
    private lateinit var statisticsDataset: SQLiteDatabase

    init {
        this.context = context
        readSQLiteDatabase(dbPath)

        val currentTimestamp = System.currentTimeMillis() / 1000
        if (::statisticsDataset.isInitialized) getDaysFromPeriod(calculateCurrentMonthStartTimestamps(), currentTimestamp)

    }

    private fun readSQLiteDatabase(uri: Uri) {
        try {
            val inputStream = this.context.contentResolver.openInputStream(uri)
            val cacheDir = this.context.cacheDir
            val tempFile = File(cacheDir, "temp_database.sqlite")
            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            statisticsDataset = SQLiteDatabase.openDatabase(
                tempFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getBooksFromPeriod(timestamp_start: Long, timestamp_end: Long){
        val sql = """
            SELECT  book_tbl.title AS title,
                    count(distinct page_stat_tbl.page) AS read_pages,
                    sum(page_stat_tbl.duration) AS total_duration,
                    book_tbl.id
            FROM    page_stat AS page_stat_tbl, book AS book_tbl
            WHERE   page_stat_tbl.id_book=book_tbl.id AND page_stat_tbl.start_time BETWEEN ? AND ?
            GROUP   BY book_tbl.id
            ORDER   BY book_tbl.last_open DESC;
        """.trimIndent()

        val cursor = statisticsDataset.rawQuery(sql, arrayOf(timestamp_start.toString(), timestamp_end.toString()))

        cursor.let {
            try {
                while (cursor.moveToNext()) {
                    val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                    val distinctPages = cursor.getInt(cursor.getColumnIndexOrThrow("read_pages"))
                    val totalDuration = cursor.getLong(cursor.getColumnIndexOrThrow("total_duration"))
                    val bookId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))

                    Log.d("Calendar","Title: $title, Distinct Pages: $distinctPages, Total Duration: $totalDuration, Book ID: $bookId")
                }
            } finally {
                cursor.close()
            }
        }
    }

    private fun getDaysFromPeriod(timestamp_start: Long, timestamp_end: Long){
        val sql = """
            SELECT dates,
                   count(*)             AS pages,
                   sum(sum_duration)    AS durations,
                   start_time
            FROM   (
                        SELECT strftime('%Y-%m-%d', start_time, 'unixepoch', 'localtime') AS dates,
                               sum(duration)                                                 AS sum_duration,
                               start_time
                        FROM   page_stat
                        WHERE  start_time BETWEEN ? AND ?
                        GROUP  BY id_book, page, dates
                   )
            GROUP  BY dates
            ORDER  BY dates DESC;
        """.trimIndent()

        val cursor = statisticsDataset.rawQuery(sql, arrayOf(timestamp_start.toString(), timestamp_end.toString()))

        cursor.let {
            try {
                while (cursor.moveToNext()) {
                    val date = cursor.getString(cursor.getColumnIndexOrThrow("dates"))
                    val pages = cursor.getInt(cursor.getColumnIndexOrThrow("pages"))
                    val durations = cursor.getLong(cursor.getColumnIndexOrThrow("durations"))
                    val startTime = cursor.getLong(cursor.getColumnIndexOrThrow("start_time"))

                    Log.d("Calendar", "Date: $date, Pages: $pages, Durations: $durations, StartTime: $startTime")
                }
            } finally {
                cursor.close()
            }
        }
    }

    fun getTodayBootStats(){
        val sql = """
                SELECT count(*),
                       sum(sum_duration)
                FROM   (
                           SELECT sum(duration) AS sum_duration
                           FROM   page_stat
                           WHERE  start_time >= ?
                           GROUP  BY id_book, page
                       );
            """.trimIndent()

        val startTime = calculateStartOfDayTimestamp()
        val cursor = statisticsDataset.rawQuery(sql, arrayOf(startTime.toString()))

        if (cursor.moveToFirst()) {
            val count = cursor.getInt(0)  // 获取 count(*)
            val totalDuration = cursor.getLong(1)  // 获取 sum(sum_duration)
            cursor.close()

            Log.d("Calendar","Count: $count")
            Log.d("Calendar","Total Duration: $totalDuration")
        } else {
            Log.d("Calendar","No data found for the given start_time.")
        }
    }

    private fun calculateStartOfDayTimestamp(): Long {
        val nowStamp = System.currentTimeMillis() / 1000
        val calendar = Calendar.getInstance()
        val nowHour = calendar.get(Calendar.HOUR_OF_DAY)
        val nowMinute = calendar.get(Calendar.MINUTE)
        val nowSecond = calendar.get(Calendar.SECOND)

        val fromBeginDay = nowHour * 3600 + nowMinute * 60 + nowSecond
        val startTodayTime = nowStamp - fromBeginDay - 24 * 3600

        return startTodayTime
    }

    private fun calculateCurrentMonthStartTimestamps(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonthTimestamp = calendar.timeInMillis / 1000

        return startOfMonthTimestamp
    }
}