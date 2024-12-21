package com.example.kobowidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import com.example.kobowidget.KoReadingStatisticsDBHandler.DayStat
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Calendar

/**
 * Implementation of App Widget functionality.
 */
class CalendarWidget : AppWidgetProvider() {

    companion object{
        const val ACTION_UPDATE_CALENDAR = "com.example.kobowidget.UPDATE_CALENDAR"
    }

    private var statisticsHandler: KoReadingStatisticsDBHandler? = null
    private var targetReadingSeconds: Long = 3600

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            // Update the widget layout
            val views = RemoteViews(context.packageName, R.layout.calendar_widget)
            updateCalendar(context, views)

        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_UPDATE_CALENDAR) {
            Log.d("Calendar", "onReceive update calendar")
            val views = RemoteViews(context.packageName, R.layout.calendar_widget)
            updateCalendar(context, views)
        }
    }

    private fun updateCalendar(
        context: Context,
        widgetViews: RemoteViews
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, CalendarWidget::class.java)

        val sharedPreferences = context.getSharedPreferences("calendar_preference", Context.MODE_PRIVATE)
        val koReadingStatisticsDBPath = sharedPreferences.getString("reading_statistics_db_path", null)

        var dayStats: MutableList<DayStat>? = null
        koReadingStatisticsDBPath.let{
            try {
                statisticsHandler = KoReadingStatisticsDBHandler(context, Uri.parse(koReadingStatisticsDBPath))
                dayStats = statisticsHandler?.getThisMonthDayStats()
            } catch (e: Exception){
                println("Error accessing Koreader Statistics DB file ${koReadingStatisticsDBPath}: ${e.message}")
            }
        }
        if (dayStats != null) drawDayCells(context, widgetViews, dayStats!!)

        // Create an intent for the widget to open when clicked
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        // Set a click listener for the widget
        widgetViews.setOnClickPendingIntent(R.id.calendar_days_layout, pendingIntent)
        // Update the widget
        appWidgetManager.updateAppWidget(componentName, widgetViews)
    }

    fun getById(
        context: Context,
        layoutId: Int
    ) = RemoteViews(context.packageName, layoutId)

    fun drawDayCells(
        context: Context,
        widgetViews: RemoteViews,
        dayStats: MutableList<DayStat>
    ) {
        val currentDate = LocalDate.now()
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val monthTitle = "${currentYear}年${currentMonth + 1}月"

        // Get the first day of the month and number of days in the month
        val firstDayOfMonth = currentDate.withDayOfMonth(1)
        val firstDayOfWeekInCurrentMonth: DayOfWeek = firstDayOfMonth.dayOfWeek
        val totalDaysOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH) // Get total number of days

        // Set the month title
        widgetViews.setTextViewText(R.id.monthTitle, monthTitle)
        // Clear existing grid
        val calendarDayBoard = getById(context, R.layout.calendar_day_board)
        calendarDayBoard.removeAllViews(R.id.calendar_days_layout)

        val cellDayFull = getById(context, R.layout.cell_day_full)
        val cellDayHalf = getById(context, R.layout.cell_day_half)
        val cellDayEmpty = getById(context, R.layout.cell_empty)
        val cellDayFuture = getById(context, R.layout.cell_day_future)

        // set the background the image view in cellDay to be null

        // Add days to the grid based on the first day of the month
        var day = 0
        for (row in 0 until 6){
            for (column in 0 until 7) {
                if (row == 0 && column < (firstDayOfWeekInCurrentMonth.value % 7)) {
                    // Fill empty spaces before the first day of the month
                    calendarDayBoard.addView(R.id.calendar_days_layout, cellDayEmpty)
                } else if (day < totalDaysOfMonth) {
                    if (day < dayStats.size) {
                        if (dayStats[day].durations > targetReadingSeconds)
                            calendarDayBoard.addView(R.id.calendar_days_layout, cellDayFull)
                        else if (dayStats[day].durations < targetReadingSeconds)
                            calendarDayBoard.addView(R.id.calendar_days_layout, cellDayHalf)
                    }
                    else {
                        calendarDayBoard.addView(R.id.calendar_days_layout, cellDayFuture)
                    }
                    day += 1
                }
            }
        }

        // Clear the calendar board before adding
        widgetViews.removeAllViews(R.id.calendar_board)
        widgetViews.addView(R.id.calendar_board, calendarDayBoard)
    }
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