package com.example.kobowidget

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RemoteViews

/**
 * Implementation of App Widget functionality.
 */
class CurrentBookWidgetDrawer (
    private val context: Context,
    private val statisticsHandler: KoReadingStatisticsDBHandler?,
    private val bookInfoDBHandler: KoReaderBookInfoDBHandler?,
    private val historyHandler: KoReaderHistoryHandler?
) : WidgetDrawer() {

    override fun getWidgetViewRemote(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_currentbook)
    }

    override fun drawContentRemote(
        widgetViews: RemoteViews
    ) {

    }

    override fun getWidgetViewMain(): LinearLayout {
        val inflater = LayoutInflater.from(context)
        val widgetView = inflater.inflate(R.layout.widget_currentbook, null) as LinearLayout
        return widgetView
    }

    override fun drawContentMain(
        widgetView: LinearLayout
    ) {
        val inflater = LayoutInflater.from(context)
        val currentBookCoverView = widgetView.findViewById<ImageView>(R.id.current_book_cover)

        if (historyHandler != null && bookInfoDBHandler != null) {
            val currentBook = historyHandler.getCurrentReadingBook()
            currentBook?.let {
                val currentBookInfo =
                    bookInfoDBHandler.getBookInfoByFilename(currentBook.filename)
                currentBookInfo?.let {
                    currentBookCoverView.setImageBitmap(currentBookInfo.getCoverBitmap())
                }
            }
        }
    }
}