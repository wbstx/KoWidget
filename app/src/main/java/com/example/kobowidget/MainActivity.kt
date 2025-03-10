package com.example.kobowidget

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.example.kobowidget.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private var bookInfoDBHandler: KoReaderBookInfoDBHandler? = null
    private var statisticsHandler: KoReadingStatisticsDBHandler? = null
    private var historyHandler: KoReaderHistoryHandler? = null

    private lateinit var calendarDrawer: CalendarWidgetDrawer
    private lateinit var currentBookDrawer: CurrentBookWidgetDrawer

    private var settingKoreaderDBHandlersLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                persistAccessPermission(uri, "koreader_path")
                setDBHandlers(uri)

                binding.let{
                    binding.settingGeneralDbPathContent.text = convertReadablePath(uri)
                }
            }
        }
    }

    private var settingBooksDirectoryPermissionLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                persistAccessPermission(uri,"books_path")
                binding.let{
                    binding.settingGeneralBooksPathContent.text = convertReadablePath(uri)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadPreference()
        updateWidgetPreview()
        setOptionsListeners(binding)
    }

    private fun setOptionsListeners(binding: ActivityMainBinding) {
        /////////////////////////
        // General
        /////////////////////////
        binding.settingGeneralKoreaderPath.setOnClickListener{ view ->
            findKoreaderDB()
        }

        binding.settingGeneralBooksPath.setOnClickListener{ view ->
            findBooksDirectory()
        }
    }

    private fun loadPreference() {
        /////////////////////////
        // General
        /////////////////////////

        // Load the koreader path in the preference
        val generalPreferences = getSharedPreferences("general_preference", Context.MODE_PRIVATE)
        val koreaderUriString = generalPreferences.getString("koreader_path", null)
        val bookDirectoryString = generalPreferences.getString("books_path", null)

        koreaderUriString?.let {
            val dbPaths = setDBHandlers(Uri.parse(koreaderUriString))
            val koReaderStatisticsDBPath = dbPaths[0]
            val koReaderBookInfoDBPath = dbPaths[1]
            val koReaderHistoryPath = dbPaths[2]

            // Set up the koreader path state icon
            if (koReaderStatisticsDBPath != null && koReaderBookInfoDBPath != null) {
                val drawable: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_check)
                binding.icDbPathState.setImageDrawable(drawable)
            }
            else{
                val drawable: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_cross)
                binding.icDbPathState.setImageDrawable(drawable)
            }
            if (koreaderUriString != null) binding.settingGeneralDbPathContent.text = convertReadablePath(Uri.parse(koreaderUriString))
            if (bookDirectoryString != null) binding.settingGeneralBooksPathContent.text = convertReadablePath(Uri.parse(bookDirectoryString))
        }

        /////////////////////////
        // Calendar
        /////////////////////////

    }

    private fun findKoreaderDB() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        settingKoreaderDBHandlersLauncher.launch(intent)
    }

    private fun findBooksDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        settingBooksDirectoryPermissionLauncher.launch(intent)
    }

    // Update the preview of the widget in the main page
    private fun updateWidgetPreview() {
        if (statisticsHandler != null) {
            val widgetContainer = findViewById<FrameLayout>(R.id.widgetContainer)
            widgetContainer.removeAllViews()

            calendarDrawer = CalendarWidgetDrawer(this, statisticsHandler)
            val calendarWidgetView = calendarDrawer.getWidgetViewMain()
            calendarDrawer.drawContentMain(calendarWidgetView)

            currentBookDrawer = CurrentBookWidgetDrawer(this, statisticsHandler,
                    bookInfoDBHandler, historyHandler)
            val currentBookWidgetView = currentBookDrawer.getWidgetViewMain()
            currentBookDrawer.drawContentMain(currentBookWidgetView)

            calendarWidgetView.apply { gravity = Gravity.CENTER }
            currentBookWidgetView.apply { gravity = Gravity.CENTER }
//            widgetContainer.addView(calendarWidgetView)
            widgetContainer.addView(currentBookWidgetView)
        }
    }

    private fun getDocumentUriInFolder(folderUri: Uri?, filePath: String): Uri? {
        folderUri?.let {
            val treeDocumentId = DocumentsContract.getTreeDocumentId(folderUri)
            val targetDocumentId = "$treeDocumentId/$filePath"
            val targetDocumentPath = DocumentsContract.buildDocumentUriUsingTree(folderUri, targetDocumentId)
            Log.d("DocumentFile", "$folderUri, $filePath")

            val documentFile = DocumentFile.fromSingleUri(this, targetDocumentPath)
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

    private fun setDBHandlers(koreaderUri: Uri): Array<Uri?> {
        val koReaderStatisticsDBPath =
            getDocumentUriInFolder(koreaderUri, "settings/statistics.sqlite3")
        val koReaderBookInfoDBPath =
            getDocumentUriInFolder(koreaderUri, "settings/bookinfo_cache.sqlite3")
        val historyPath =
            getDocumentUriInFolder(koreaderUri, "history.lua")

        val sharedPreferences =
            getSharedPreferences("general_preference", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        koReaderStatisticsDBPath?.let {
            statisticsHandler =
                KoReadingStatisticsDBHandler(this, koReaderStatisticsDBPath)

            editor.putString("reading_statistics_db_path", koReaderStatisticsDBPath.toString())
            editor.apply()
        }

        koReaderBookInfoDBPath?.let {
            bookInfoDBHandler =
                KoReaderBookInfoDBHandler(this, koReaderBookInfoDBPath)

            editor.putString("bookinfo_db_path", koReaderBookInfoDBPath.toString())
            editor.apply()
        }

        historyPath?.let {
            historyHandler = KoReaderHistoryHandler(this, historyPath)

            editor.putString("history_path", historyPath.toString())
            editor.apply()
        }

        return arrayOf(koReaderStatisticsDBPath, koReaderBookInfoDBPath, historyPath)
    }

    // Get the persistent permission to the db file
    private fun persistAccessPermission(uri: Uri, path_name: String) {
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            println("Persistable $uri Permission taken successfully")

            val sharedPreferences = getSharedPreferences("general_preference", Context.MODE_PRIVATE)
            sharedPreferences.edit().putString(path_name, uri.toString()).apply()

        } catch (e: SecurityException) {
            e.printStackTrace()
            println("Error taking persistable URI permission: ${e.message}")
        }
    }

    private fun convertReadablePath(uri: Uri): String? {
        if ("com.android.externalstorage.documents" == uri.authority) {
            val docId = DocumentsContract.getTreeDocumentId(uri) // 获取 Document ID
            val split = docId.split(":")
            val type = split[0]
            val relativePath = split.getOrNull(1) ?: ""

            return if ("primary" == type) {
                "/sdcard/$relativePath"
            } else {
                "/storage/$type/$relativePath"
            }
        }
        return null
    }
}