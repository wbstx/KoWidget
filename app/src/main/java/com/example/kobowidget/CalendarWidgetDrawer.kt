package com.example.kobowidget

import android.content.Context
import android.view.LayoutInflater
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Calendar

/**
 * Implementation of App Widget functionality.
 */
class CalendarWidgetDrawer(
    private val context: Context,
    private val statisticsHandler: KoReadingStatisticsDBHandler?
): WidgetDrawer() {
    private var targetReadingSeconds: Long = 3600

    override fun drawDayCellsRemote(
        widgetViews: RemoteViews
    ) {
        val dayStats = statisticsHandler?.getThisMonthDayStats()

        dayStats?.let {
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
            val calendarDayBoard = getById(context, R.layout.widget_component_calendar_day_board)
            calendarDayBoard.removeAllViews(R.id.calendar_days_layout)

            val cellDayFull = getById(context, R.layout.cell_calendar_day_full)
            val cellDayHalf = getById(context, R.layout.cell_calendar_day_half)
            val cellDayEmpty = getById(context, R.layout.cell_calendar_day_empty)
            val cellDayNoReadOrFuture = getById(context, R.layout.cell_calendar_day_future)

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
                            if (dayStats[day].durations.toInt() == 0)
                                calendarDayBoard.addView(R.id.calendar_days_layout, cellDayNoReadOrFuture)
                            else if (dayStats[day].durations > targetReadingSeconds)
                                calendarDayBoard.addView(R.id.calendar_days_layout, cellDayFull)
                            else if (dayStats[day].durations < targetReadingSeconds)
                                calendarDayBoard.addView(R.id.calendar_days_layout, cellDayHalf)
                        }
                        else {
                            calendarDayBoard.addView(R.id.calendar_days_layout, cellDayNoReadOrFuture)
                        }
                        day += 1
                    }
                }
            }

            // Clear the calendar board before adding
            widgetViews.removeAllViews(R.id.calendar_board)
            val weekHead = getById(context, R.layout.widget_component_calendar_week_head)
            widgetViews.addView(R.id.calendar_board, weekHead)
            widgetViews.addView(R.id.calendar_board, calendarDayBoard)
        }
    }

    override fun drawDayCellsMain(
        widgetView: LinearLayout
    ) {
        val inflater = LayoutInflater.from(context)
        val dayStats = statisticsHandler?.getThisMonthDayStats()

        dayStats?.let {
            val currentDate = LocalDate.now()
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            val monthTitle = "${currentYear}年${currentMonth + 1}月"

            // Get the first day of the month and number of days in the month
            val firstDayOfMonth = currentDate.withDayOfMonth(1)
            val firstDayOfWeekInCurrentMonth: DayOfWeek = firstDayOfMonth.dayOfWeek
            val totalDaysOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH) // Get total number of days

            val monthTitleView = widgetView.findViewById<TextView>(R.id.monthTitle)
            monthTitleView.text = monthTitle

            val calendarBoard = widgetView.findViewById<LinearLayout>(R.id.calendar_board)
            val weekHeadView = inflater.inflate(R.layout.widget_component_calendar_week_head, calendarBoard, false)
            calendarBoard.addView(weekHeadView)

            // Clear existing grid
            val calendarDayBoard = inflater.inflate(R.layout.widget_component_calendar_day_board, calendarBoard, false) as GridLayout

            // Add days to the grid based on the first day of the month
            var day = 0
            for (row in 0 until 6){
                for (column in 0 until 7) {
                    if (row == 0 && column < (firstDayOfWeekInCurrentMonth.value % 7)) {
                        // Fill empty spaces before the first day of the month
                        calendarDayBoard.addView(getLayoutById(inflater, R.layout.cell_calendar_day_empty, calendarDayBoard))
                    } else if (day < totalDaysOfMonth) {
                        if (day < dayStats.size) {
                            if (dayStats[day].durations.toInt() == 0)
                                calendarDayBoard.addView(getLayoutById(inflater, R.layout.cell_calendar_day_future, calendarDayBoard))
                            else if (dayStats[day].durations > targetReadingSeconds)
                                calendarDayBoard.addView(getLayoutById(inflater, R.layout.cell_calendar_day_full, calendarDayBoard))
                            else if (dayStats[day].durations < targetReadingSeconds)
                                calendarDayBoard.addView(getLayoutById(inflater, R.layout.cell_calendar_day_half, calendarDayBoard))
                        }
                        else {
                            calendarDayBoard.addView(getLayoutById(inflater, R.layout.cell_calendar_day_future, calendarDayBoard))
                        }
                        day += 1
                    }
                }
            }

            // Clear the calendar board before adding
//            widgetView.removeAllViews(R.id.calendar_board)
//            val weekHead = getById(context, R.layout.widget_component_calendar_week_head)
//            widgetViews.addView(R.id.calendar_board, weekHead)
            calendarBoard.addView(calendarDayBoard)
        }
    }
}