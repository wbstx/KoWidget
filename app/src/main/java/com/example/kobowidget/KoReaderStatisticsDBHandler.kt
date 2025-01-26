package com.example.kobowidget

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class KoReadingStatisticsDBHandler (
    private var context: Context,
    dbPath: Uri
) {
    lateinit var statisticsDataset: SQLiteDatabase

    init {
        readSQLiteDatabase(dbPath)
    }

    fun getThisMonthDayStats(): MutableList<DayStat>? {
        if (::statisticsDataset.isInitialized) {
            val currentTimestamp = System.currentTimeMillis() / 1000
            return retrieveDaysFromPeriod(calculateCurrentMonthStartTimestamps(), currentTimestamp)
        }
        else return null
    }

    private fun readSQLiteDatabase(uri: Uri) {
        try {
            val inputStream = this.context.contentResolver.openInputStream(uri)
            val cacheDir = this.context.cacheDir
            val tempFile = File(cacheDir, "temp_statistics.sqlite")
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

    fun retrieveBooksFromPeriod(timestampStart: Long, timestampEnd: Long){
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

        val cursor = statisticsDataset.rawQuery(sql, arrayOf(timestampStart.toString(), timestampEnd.toString()))

        cursor.let {
            cursor.use { cursor ->
                while (cursor.moveToNext()) {
                    val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                    val distinctPages = cursor.getInt(cursor.getColumnIndexOrThrow("read_pages"))
                    val totalDuration = cursor.getLong(cursor.getColumnIndexOrThrow("total_duration"))
                    val bookId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))

                    Log.d("Calendar","Title: $title, Distinct Pages: $distinctPages, Total Duration: $totalDuration, Book ID: $bookId")
                }
            }
        }
    }

    fun retrieveDaysFromPeriod(timestampStart: Long, timestampEnd: Long): MutableList<DayStat>? {
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

        val cursor = statisticsDataset.rawQuery(sql, arrayOf(timestampStart.toString(), timestampEnd.toString()))

        val dayStats: MutableList<DayStat> = mutableListOf()
        cursor.let {
            cursor.use { cursor ->
                while (cursor.moveToNext()) {
                    val date = cursor.getString(cursor.getColumnIndexOrThrow("dates"))
                    val pages = cursor.getInt(cursor.getColumnIndexOrThrow("pages"))
                    val durations = cursor.getLong(cursor.getColumnIndexOrThrow("durations"))
                    val startTime = cursor.getLong(cursor.getColumnIndexOrThrow("start_time"))

                    dayStats += DayStat(date, pages, durations, startTime)
                }
            }

            dayStats.sortBy { it.date }

            val completeDayStats: MutableList<DayStat> = mutableListOf()
            for (i in dayStats.indices) {
                if (i > 0) {
                    val previousDate = dayStats[i - 1].date.takeLast(2).toInt()
                    val currentDate = dayStats[i].date.takeLast(2).toInt()

                    var missingDate = previousDate + 1
                    while (missingDate < currentDate) {
                        val newDate = dayStats[i - 1].date.dropLast(2) + String.format("%02d", missingDate)
                        Log.d("Calendar", "Date ${newDate}, Pages: 0, Durations: 0, StartTime: -1")
                        completeDayStats.add(DayStat(newDate, 0, 0, -1))
                        missingDate++
                    }
                }
                Log.d("Calendar", "Date ${dayStats[i].date}, Pages: ${dayStats[i].pages}, Durations: ${dayStats[i].durations}, StartTime: ${dayStats[i].startTime}")
                completeDayStats.add(dayStats[i])
            }
            return completeDayStats
        }
        return null
    }

    fun retrieveTodayBootStats(){
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

    @SuppressLint("Range")
    fun getBookStat(bookId: Int): BookInfo? {
        val totalReadDaysSQL = """
            SELECT count(*)
            FROM   (
                        SELECT strftime('%%Y-%%m-%%d', start_time, 'unixepoch', 'localtime') AS dates
                        FROM   page_stat
                        WHERE  id_book = ?
                        GROUP  BY dates
                   );
            """.trimIndent()

        val totalReadDaysCursor = statisticsDataset.rawQuery(totalReadDaysSQL, arrayOf(bookId.toString()))

        totalReadDaysCursor.use {
            if (totalReadDaysCursor.moveToFirst()) {
                val count = totalReadDaysCursor.getInt(0)
                Log.d("BookInfo","Unique Dates for Book ID $bookId: $count")
            } else {
                Log.d("BookInfo","$bookId not found")
            }
        }

        val bookPagesSQL = """
            SELECT sum(duration),
                   count(DISTINCT page),
                   min(start_time),
                   (SELECT max(ps2.page) 
                    FROM page_stat AS ps2 
                    WHERE ps2.start_time = (SELECT max(start_time) FROM page_stat WHERE id_book = ?))
            FROM page_stat
            WHERE id_book = ?;
            """.trimIndent()

        val bookPagesCursor = statisticsDataset.rawQuery(bookPagesSQL, arrayOf(bookId.toString(), bookId.toString()))
        bookPagesCursor.use {
            if (bookPagesCursor.moveToFirst()) {
                val totalTimeBook = bookPagesCursor.getLong(0)  // sum(duration)
                val totalReadPages = bookPagesCursor.getInt(1)  // count(DISTINCT page)
                val firstOpen = bookPagesCursor.getLong(2)      // min(start_time)
                val lastPage = bookPagesCursor.getInt(3)        // max(page) from the last open time

                Log.d("BookInfo","Total Time: $totalTimeBook, ${totalTimeBook / 60}")
                Log.d("BookInfo","Total Read Pages: $totalReadPages")
                Log.d("BookInfo","First Open Time: $firstOpen")
                Log.d("BookInfo","Last Page at Last Open Time: $lastPage")
            } else {
                Log.d("BookInfo","No data found for book ID $bookId.")
            }
        }

        return null
    }

    fun calculateStartOfDayTimestamp(): Long {
        val nowStamp = System.currentTimeMillis() / 1000
        val calendar = Calendar.getInstance()
        val nowHour = calendar.get(Calendar.HOUR_OF_DAY)
        val nowMinute = calendar.get(Calendar.MINUTE)
        val nowSecond = calendar.get(Calendar.SECOND)

        val fromBeginDay = nowHour * 3600 + nowMinute * 60 + nowSecond
        val startTodayTime = nowStamp - fromBeginDay - 24 * 3600

        return startTodayTime
    }

    fun calculateCurrentMonthStartTimestamps(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonthTimestamp = calendar.timeInMillis / 1000

        return startOfMonthTimestamp
    }

    data class DayStat(
        val date: String,
        val pages: Int,
        val durations: Long,
        val startTime: Long
    )
}