package com.example.kobowidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.media.Image
import android.util.Log
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Calendar

/**
 * Implementation of App Widget functionality.
 */
class CalendarWidget : AppWidgetProvider() {
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
        for (appWidgetId in appWidgetIds) {
            // Update the widget layout
            val views = RemoteViews(context.packageName, R.layout.calendar_widget)
            drawDayCells(context, views)

//
//            // Add days to the grid
//            var day = 1
//            for (row in 0..5) { // 6 rows max
//                for (col in 0..6) { // 7 columns
//                    if (row == 0 && col < firstDayOfWeek - 1) {
//                        // Fill empty spaces before the first day of the month
//                        val emptyView = TextView(context)
//                        emptyView.text = ""
//                        gridLayout.addView(emptyView)
//                    } else if (day <= totalDays) {
//                        // Add the date to the grid
//                        val dayView = TextView(context)
//                        dayView.text = "$day"
//                        gridLayout.addView(dayView)
//                        day++
//                    }
//                }
//            }

            // Set a click listener for the widget
//            views.setOnClickPendingIntent(R.id.widgetLayout, pendingIntent)

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

    firstDayOfWeekInCurrentMonth = DayOfWeek.WEDNESDAY
    Log.d("Calendar", "$firstDayOfMonth, $totalDaysOfMonth,  $firstDayOfWeekInCurrentMonth, ${firstDayOfWeekInCurrentMonth.value}")

    // Add days to the grid based on the first day of the month
    var day = 0
    for (row in 0 until 6){
        for (column in 0 until 7) {
            if (row == 0 && column < firstDayOfWeekInCurrentMonth.value) {
                // Fill empty spaces before the first day of the month
                calendarDayBoard.addView(R.id.calendar_days_layout, cellEmpty)
                // not adding up days
                continue
            } else if (day < totalDaysOfMonth) {
                // Add the date to the grid
                cellDay.setInt(
                    R.id.cell_day,
                    "setBackgroundResource",
                    R.drawable.circle_with_border
                )
                calendarDayBoard.addView(R.id.calendar_days_layout, cellDay)
            }
            day += 1
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