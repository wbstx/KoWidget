package com.example.kobowidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.media.Image
import android.net.Uri
import android.util.Log
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.content.contentValuesOf
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Calendar
import kotlin.math.exp

/**
 * Implementation of App Widget functionality.
 */
class CalendarWidget : AppWidgetProvider() {

    private lateinit var StatisticsHandler: KoReadingStatisticsDBHandler

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // Create an intent for the widget to open when clicked
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

//        // There may be multiple widgets active, so update all of them
//        for (appWidgetId in appWidgetIds) {
//            updateAppWidget(context, appWidgetManager, appWidgetId)
//        }

        val sharedPreferences = context.getSharedPreferences("calendar_preference", Context.MODE_PRIVATE)
        val koReadingStatisticsDBPath = sharedPreferences.getString("reading_statistics_db_path", null)

        if (koReadingStatisticsDBPath != null) {
            try {
                StatisticsHandler = KoReadingStatisticsDBHandler(context, Uri.parse(koReadingStatisticsDBPath))
            } catch (e: Exception){
                println("Error accessing Koreader Statistics DB file ${koReadingStatisticsDBPath}: ${e.message}")
            }
        }

        for (appWidgetId in appWidgetIds) {
            // Update the widget layout
            val views = RemoteViews(context.packageName, R.layout.calendar_widget)
            drawDayCells(context, views)

            // Set a click listener for the widget
            views.setOnClickPendingIntent(R.id.calendar_days_layout, pendingIntent)

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

fun getById(
    context: Context,
    layoutId: Int
) = RemoteViews(context.packageName, layoutId)

fun drawDayCells(
    context: Context,
    widgetViews: RemoteViews
) {
    val currentDate = LocalDate.now()
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    val monthTitle = "${currentYear}年${currentMonth + 1}月"

    // Get the first day of the month and number of days in the month
    val firstDayOfMonth = currentDate.withDayOfMonth(1)
    var firstDayOfWeekInCurrentMonth: DayOfWeek = firstDayOfMonth.dayOfWeek
    val totalDaysOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH) // Get total number of days

    // Set the month title
    widgetViews.setTextViewText(R.id.monthTitle, monthTitle)
    // Clear existing grid
    val calendarDayBoard = getById(context, R.layout.calendar_day_board)
    calendarDayBoard.removeAllViews(R.id.calendar_days_layout)
    var cellDay = getById(context, R.layout.cell_day)
    var cellEmpty = getById(context, R.layout.cell_empty)
    // set the background the image view in cellDay to be null

    // Add days to the grid based on the first day of the month
    var day = 0
    for (row in 0 until 6){
        for (column in 0 until 7) {
            if (row == 0 && column < (firstDayOfWeekInCurrentMonth.value % 7)) {
                // Fill empty spaces before the first day of the month
                calendarDayBoard.addView(R.id.calendar_days_layout, cellEmpty)
            } else if (day < totalDaysOfMonth) {
                calendarDayBoard.addView(R.id.calendar_days_layout, cellDay)
                day += 1
            }
        }
    }

    widgetViews.addView(R.id.calendar_frame, calendarDayBoard)
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val widgetText = context.getString(R.string.appwidget_text)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.calendar_widget)
//    views.setTextViewText(R.id.appwidget_text, widgetText)

    val childRemoteViews = RemoteViews(context.packageName, R.layout.calendar_day_board)
    views.addView(R.id.calendar_days_layout, childRemoteViews)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}