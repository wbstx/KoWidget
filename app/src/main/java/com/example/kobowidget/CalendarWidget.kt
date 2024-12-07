package com.example.kobowidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.content.ContextCompat
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

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val monthTitle = "${currentYear}年${currentMonth + 1}月"

        // Get the first day of the month and number of days in the month
        calendar.set(currentYear, currentMonth, 1)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Get the first day of the month
        val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH) // Get total number of days

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

            val calendarDay = getById(context, R.layout.test)
            var CelldayRemoteView = getById(context, R.layout.cell_day)
            calendarDay.addView(R.id.calendar_days_layout, CelldayRemoteView)
            calendarDay.addView(R.id.calendar_days_layout, CelldayRemoteView)
            calendarDay.addView(R.id.calendar_days_layout, CelldayRemoteView)
            calendarDay.addView(R.id.calendar_days_layout, CelldayRemoteView)
            calendarDay.addView(R.id.calendar_days_layout, CelldayRemoteView)
            calendarDay.addView(R.id.calendar_days_layout, CelldayRemoteView)

            views.addView(R.id.calendar_frame, calendarDay)

//            // Set the month title
//            views.setTextViewText(R.id.monthTitle, monthTitle)
//            // Clear existing grid
//            val gridLayout = getById(context, R.layout.cell_day)
//            gridLayout.removeAllViews()
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


//internal fun updateAppWidget(
//    context: Context,
//    appWidgetManager: AppWidgetManager,
//    appWidgetId: Int
//) {
//    val widgetText = context.getString(R.string.appwidget_text)
//    // Construct the RemoteViews object
//    val views = RemoteViews(context.packageName, R.layout.calendar_widget)
////    views.setTextViewText(R.id.appwidget_text, widgetText)
//
//    val childRemoteViews = RemoteViews(context.packageName, R.layout.test)
//    views.addView(R.id.calendar_days_layout, childRemoteViews)
//
//    // Instruct the widget manager to update the widget
//    appWidgetManager.updateAppWidget(appWidgetId, views)
//}