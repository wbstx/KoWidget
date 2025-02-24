package com.example.kobowidget

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import org.w3c.dom.Text

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
        val currentBookTitleView = widgetView.findViewById<TextView>(R.id.current_book_title)
        val currentBookAuthorView = widgetView.findViewById<TextView>(R.id.current_book_author)
        val currentBookReadTimeView = widgetView.findViewById<TextView>(R.id.current_book_read_time)

        if (historyHandler != null && bookInfoDBHandler != null && statisticsHandler != null) {
            val currentBook = historyHandler.getCurrentReadingBook()
            currentBook?.let {
                val currentBookInfo =
                    bookInfoDBHandler.getBookInfoByFilename(currentBook.filename)
                currentBookInfo?.let {
                    currentBookCoverView.setImageBitmap(currentBookInfo.getCoverBitmap())
                    currentBookTitleView.text = currentBookInfo.title
                    currentBookAuthorView.text = currentBookInfo.authors

                    val currentBookMD5 = currentBookInfo.md5!!
                    val currentBookStatID = statisticsHandler.getBookStatID(currentBookInfo.title, currentBookInfo.authors, currentBookMD5)
                    currentBookStatID?.let {
                        Log.d("Bookinfo", "Current Book Stat ID is $currentBookStatID")
                        val bookStat = statisticsHandler.getBookStatByStatID(currentBookStatID)
                        bookStat?.let{
                            currentBookReadTimeView.text = convertSecondsToHMS(bookStat.totalReadTime)
                        }
                    }
                }
            }
        }
    }

    fun convertSecondsToHMS(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }
}