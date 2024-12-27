package com.example.kobowidget

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.example.kobowidget.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var openDocumentTreeLauncher: ActivityResultLauncher<Intent>

    private var bookInfoDBHandler: KoReaderBookInfoDBHandler? = null
    private var statisticsHandler: KoReadingStatisticsDBHandler? = null

    private lateinit var calendarDrawer: CalendarWidgetDrawer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        setSupportActionBar(binding.toolbar)

//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)

        loadPreference()
//        updateWidgetPreview()
        setOptionsListeners(binding)
    }

    private fun setOptionsListeners(binding: ActivityMainBinding) {
        /////////////////////////
        // General
        /////////////////////////
        openDocumentTreeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    persistAccessPermission(uri)

                    val dbPaths = setDBPathOptionAndIcon(uri)
                    val koReadingStatisticsDBPath = dbPaths[0]
                    val koReadingBookInfoDBPath = dbPaths[1]

                    // save the statistics db path into preference
                    if (koReadingStatisticsDBPath != null){
                        // Save the koreader statistics db path into the preference
                        val sharedPreferences = getSharedPreferences("calendar_preference", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("reading_statistics_db_path", koReadingStatisticsDBPath.toString())
                        editor.apply()
                    }
                    binding.settingGeneralDbPathContent.text = convertReadablePath(uri)
                }
            }
        }

        binding.settingGeneralDbPath.setOnClickListener{ view ->
            findKoreaderDB()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun loadPreference() {
        /////////////////////////
        // General
        /////////////////////////

        // Load the koreader path in the preference
        val generalPreferences = getSharedPreferences("general_preference", Context.MODE_PRIVATE)
        val koreaderUriString = generalPreferences.getString("koreader_path", null)
        koreaderUriString?.let {
            setDBPathOptionAndIcon(Uri.parse(koreaderUriString))
        }

        /////////////////////////
        // Calendar
        /////////////////////////
        val sharedPreferences =
            this.getSharedPreferences("calendar_preference", Context.MODE_PRIVATE)
        val koReadingStatisticsDBPath =
            sharedPreferences.getString("reading_statistics_db_path", null)
        Log.d("calendar widget", "$koReadingStatisticsDBPath")

        koReadingStatisticsDBPath.let {
            try {
                statisticsHandler =
                    KoReadingStatisticsDBHandler(this, Uri.parse(koReadingStatisticsDBPath))
            } catch (e: Exception) {
                println("Error accessing Koreader Statistics DB file ${koReadingStatisticsDBPath}: ${e.message}")
            }
        }
    }

    private fun findKoreaderDB() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        openDocumentTreeLauncher.launch(intent)
    }

    private fun updateWidgetPreview() {
        if (statisticsHandler != null) {
            val widgetContainer = findViewById<FrameLayout>(R.id.widgetContainer)
            widgetContainer.removeAllViews()
            var widgetView = layoutInflater.inflate(R.layout.widget_calendar, widgetContainer, false) as LinearLayout

            calendarDrawer = CalendarWidgetDrawer(this, statisticsHandler)
            calendarDrawer.drawDayCellsMain(widgetView)

            widgetView = widgetView.apply { gravity = Gravity.CENTER }
            widgetContainer.addView(widgetView)
        }
    }

    private fun getDocumentUriInFolder(folderUri: Uri?, filePath: String): Uri? {
        folderUri?.let {
            val treeDocumentId = DocumentsContract.getTreeDocumentId(folderUri)
            val targetDocumentId = "$treeDocumentId/$filePath"
            val targetDocumentPath = DocumentsContract.buildDocumentUriUsingTree(folderUri, targetDocumentId)

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

    private fun setDBPathOptionAndIcon(koreaderUri: Uri): Array<Uri?> {
        val koReaderStatisticsDBPath = getDocumentUriInFolder(koreaderUri, "settings/statistics.sqlite3")
        val koReaderBookInfoDBPath = getDocumentUriInFolder(koreaderUri, "settings/bookinfo_cache.sqlite3")

        bookInfoDBHandler = KoReaderBookInfoDBHandler(this, koReaderBookInfoDBPath!!)
        val bookCover = bookInfoDBHandler!!.printAllBook()
        bookCover?.let {
            Log.d("Bookinfo", "Setting Image Bitmap")
            val testBookCoverImageView = findViewById<ImageView>(R.id.test_book_cover)
            testBookCoverImageView.setImageBitmap(bookCover)
        }

        if (koReaderStatisticsDBPath != null && koReaderBookInfoDBPath != null){
            val drawable: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_check)
            Log.d("calendar", "Binding: ${binding.icDbPathState}")
            binding.icDbPathState.setImageDrawable(drawable)
            Log.d("calendar","drawable set $koReaderStatisticsDBPath")
            Log.d("calendar","drawable set $koReaderBookInfoDBPath")
        }
        else{
            val drawable: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_cross)
            binding.icDbPathState.setImageDrawable(drawable)
        }
        binding.settingGeneralDbPathContent.text = convertReadablePath(koreaderUri)
        return arrayOf(koReaderStatisticsDBPath, koReaderBookInfoDBPath)
    }

    // Get the persistent permission to the db file
    private fun persistAccessPermission(uri: Uri) {
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            println("Persistable URI Permission taken successfully")

            val sharedPreferences = getSharedPreferences("general_preference", Context.MODE_PRIVATE)
            sharedPreferences.edit().putString("koreader_path", uri.toString()).apply()

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