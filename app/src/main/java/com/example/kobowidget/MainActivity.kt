package com.example.kobowidget

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import com.example.kobowidget.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val OPEN_DOCUMENT_REQUEST_CODE = 100

    private lateinit var statisticsDataset: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        findKoreaderDB()

        binding.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null)
//                .setAnchorView(R.id.fab).show()
            val currentTimestamp = System.currentTimeMillis() / 1000
            if (::statisticsDataset.isInitialized) getBooksFromPeriod(calculateCurrentMonthStartTimestamps(), currentTimestamp)
            if (::statisticsDataset.isInitialized) getDaysFromPeriod(calculateCurrentMonthStartTimestamps(), currentTimestamp)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }


    private fun findKoreaderDB() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
        }
        startActivityForResult(intent, OPEN_DOCUMENT_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OPEN_DOCUMENT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                readSQLiteDatabase(uri)
            }
        }
    }

    private fun readSQLiteDatabase(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val cacheDir = cacheDir
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

                    Log.d("SQL Result", "Date: $date, Pages: $pages, Durations: $durations, StartTime: $startTime")
                }
            } finally {
                cursor.close()
            }
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