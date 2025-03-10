package com.example.kobowidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.widget.RemoteViews
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Implementation of App Widget functionality.
 */
class CurrentBookWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_CURRENT_BOOK = "com.example.kobowidget.UPDATE_CURRENT_BOOK"
    }

    private var statisticsHandler: KoReadingStatisticsDBHandler? = null
    private var bookInfoHandler: KoReaderBookInfoDBHandler? = null
    private var historyHandler: KoReaderHistoryHandler? = null

    private lateinit var currentBookDrawer: CurrentBookWidgetDrawer

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            // Update the widget layout
            updateCurrentBook(context)
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

        if (intent.action == ACTION_UPDATE_CURRENT_BOOK) {
            Log.d("Bookinfo", "onReceive update Current Book Widget")
            updateCurrentBook(context)
        }
    }

    fun updateCurrentBook(
        context: Context
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, CurrentBookWidget::class.java)

        val sharedPreferences =
            context.getSharedPreferences("general_preference", Context.MODE_PRIVATE)
        val koReadingStatisticsDBPath =
            sharedPreferences.getString("reading_statistics_db_path", null)
        val koreaderBookInfoDBPath =
            sharedPreferences.getString("bookinfo_db_path", null)
        val koreaderHistoryPath =
            sharedPreferences.getString("history_path", null)

        try {
            statisticsHandler =
                KoReadingStatisticsDBHandler(context, Uri.parse(koReadingStatisticsDBPath))
        } catch (e: Exception) {
            println("Error accessing Koreader Statistics DB file ${koReadingStatisticsDBPath}: ${e.message}")
            return
        }

        try {
            bookInfoHandler =
                KoReaderBookInfoDBHandler(context, Uri.parse(koreaderBookInfoDBPath))
        } catch (e: Exception) {
            println("Error accessing Koreader BookInfo DB file ${koreaderBookInfoDBPath}: ${e.message}")
            return
        }

        try {
            historyHandler = KoReaderHistoryHandler(context, Uri.parse(koreaderHistoryPath))
        } catch (e: Exception) {
            println("Error accessing History file ${koreaderHistoryPath}: ${e.message}")
            return
        }

        currentBookDrawer = CurrentBookWidgetDrawer(context, statisticsHandler,
            bookInfoHandler, historyHandler)
        val widgetViews = currentBookDrawer.getWidgetViewRemote()
        currentBookDrawer.drawContentRemote(widgetViews)
        Log.d("Bookinfo","Widget Done Drawing")

        val currentBookFilePath = historyHandler!!.getCurrentReadingBook()!!.filename
        val currentBookFileName = currentBookFilePath.substringAfterLast("/")

//        val intent = Intent(context, OpenEpubActivity::class.java).apply {
//            action = "com.example.OPEN_EPUB"
//            putExtra("filePath", currentBookFilePath)
//            putExtra("fileName", currentBookFileName)
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            context, 0, intent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
        val pendingIntent = createOpenFileIntent(context, currentBookFilePath)
        if (pendingIntent != null)
            widgetViews.setOnClickPendingIntent(R.id.bookinfo_widget_layout, pendingIntent)

//        val intent = context.packageManager.getLaunchIntentForPackage("org.koreader.launcher")
//        if (intent != null) {
//            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or  PendingIntent.FLAG_IMMUTABLE);
//            widgetViews.setOnClickPendingIntent(R.id.bookinfo_widget_layout, pendingIntent);
//        }
//        else{
//            Log.d("Bookinfo","Koreader not found")
//        }

        // Update the widget
        appWidgetManager.updateAppWidget(componentName, widgetViews)
    }

    fun getById(
        context: Context,
        layoutId: Int
    ) = RemoteViews(context.packageName, layoutId)

    private fun getDocumentUriInFolder(context: Context, folderUri: Uri?, filePath: String): Uri? {
        folderUri?.let {
            val treeDocumentId = DocumentsContract.getTreeDocumentId(folderUri)
            val targetDocumentId = "$treeDocumentId/$filePath"
            val targetDocumentPath = DocumentsContract.buildDocumentUriUsingTree(folderUri, targetDocumentId)
            Log.d("DocumentFile", "$folderUri, $filePath")

            val documentFile = DocumentFile.fromSingleUri(context, targetDocumentPath)
            if (documentFile != null && documentFile.exists()) {
                Log.d("DocumentFile", "File exists: ${documentFile.name}")
                return targetDocumentPath
            } else {
                Log.e("DocumentFile", "File not found: $filePath")
                return null
            }
        }
        return null
    }

    private fun createOpenFileIntent(context: Context, filepath: String): PendingIntent? {
        val file = File(filepath).toUri()
        val currentBookFileName = filepath.substringAfterLast("/")
        val persistedUris = context.contentResolver.persistedUriPermissions
        if (persistedUris.isNotEmpty()) {
            val baseUri = persistedUris[1].uri
            val filenameContent = getDocumentUriInFolder(context, baseUri, currentBookFileName)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                Log.d("Bookinfo", filenameContent.toString())
                setDataAndType(filenameContent, "application/epub+zip")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            return PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        return null
        return TODO("Provide the return value")
    }
}

internal fun updateCurrentBookWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val widgetText = context.getString(R.string.appwidget_text)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.widget_calendar)
//    views.setTextViewText(R.id.appwidget_text, widgetText)

    val childRemoteViews = RemoteViews(context.packageName, R.layout.widget_component_calendar_day_board)
    views.addView(R.id.calendar_days_layout, childRemoteViews)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}