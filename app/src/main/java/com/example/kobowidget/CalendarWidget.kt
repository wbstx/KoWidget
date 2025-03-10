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

    companion object {
        const val ACTION_UPDATE_CALENDAR = "com.example.kobowidget.UPDATE_CALENDAR"
    }

    private var statisticsHandler: KoReadingStatisticsDBHandler? = null
    private var targetReadingSeconds: Long = 3600

    private lateinit var calendarDrawer: CalendarWidgetDrawer

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            // Update the widget layout
//            val views = RemoteViews(context.packageName, R.layout.widget_calendar)
            updateCalendar(context)
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
            updateCalendar(context)
        }
    }

    fun updateCalendar(
        context: Context
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, CalendarWidget::class.java)

        val sharedPreferences =
            context.getSharedPreferences("general_preference", Context.MODE_PRIVATE)
        val koReadingStatisticsDBPath =
            sharedPreferences.getString("reading_statistics_db_path", null)

        koReadingStatisticsDBPath.let {
            try {
                statisticsHandler =
                    KoReadingStatisticsDBHandler(context, Uri.parse(koReadingStatisticsDBPath))

                calendarDrawer = CalendarWidgetDrawer(context, statisticsHandler)
                val widgetViews = calendarDrawer.getWidgetViewRemote()
                calendarDrawer.drawContentRemote(widgetViews)

                // Create an intent for the widget to open when clicked
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent =
                    PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                // Set a click listener for the widget
                widgetViews.setOnClickPendingIntent(R.id.calendar_days_layout, pendingIntent)
                // Update the widget
                appWidgetManager.updateAppWidget(componentName, widgetViews)

            } catch (e: Exception) {
                println("Error accessing Koreader Statistics DB file ${koReadingStatisticsDBPath}: ${e.message}")
            }
        }
    }

    fun getById(
        context: Context,
        layoutId: Int
    ) = RemoteViews(context.packageName, layoutId)
}

//internal fun updateAppWidget(
//    context: Context,
//    appWidgetManager: AppWidgetManager,
//    appWidgetId: Int
//) {
//    val widgetText = context.getString(R.string.appwidget_text)
//    // Construct the RemoteViews object
//    val views = RemoteViews(context.packageName, R.layout.widget_calendar)
////    views.setTextViewText(R.id.appwidget_text, widgetText)
//
//    val childRemoteViews = RemoteViews(context.packageName, R.layout.widget_component_calendar_day_board)
//    views.addView(R.id.calendar_days_layout, childRemoteViews)
//
//    // Instruct the widget manager to update the widget
//    appWidgetManager.updateAppWidget(appWidgetId, views)
//}