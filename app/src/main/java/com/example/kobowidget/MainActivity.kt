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
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.example.kobowidget.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var openDocumentTreeLauncher: ActivityResultLauncher<Intent>

    private lateinit var statisticsDataset: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        setSupportActionBar(binding.toolbar)

//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)

        val generalPreferences = getSharedPreferences("general_preference", Context.MODE_PRIVATE)
        val koreaderUriString = generalPreferences.getString("koreader_path", null)
        koreaderUriString?.let {
            setDBPathOptionAndIcon(Uri.parse(koreaderUriString))
        }

        setOptionsListener(binding)

//        binding.fab.setOnClickListener { view ->
////            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
////                .setAction("Action", null)
////                .setAnchorView(R.id.fab).show()
//            val currentTimestamp = System.currentTimeMillis() / 1000
//            var statisticsHandler = KoReadingStatisticsDBHandler(this, koReadingStatisticsDBPath!!)
//            if (::statisticsDataset.isInitialized) statisticsHandler.retrieveBooksFromPeriod(statisticsHandler.calculateCurrentMonthStartTimestamps(), currentTimestamp)
//            if (::statisticsDataset.isInitialized) statisticsHandler.retrieveDaysFromPeriod(statisticsHandler.calculateCurrentMonthStartTimestamps(), currentTimestamp)
//
//            val sharedPreferences = getSharedPreferences("calendar_preference", Context.MODE_PRIVATE)
//            val editor = sharedPreferences.edit()
//            editor.putString("reading_statistics_db_path", koReadingStatisticsDBPath.toString())
//            editor.apply()
//
//            val intent = Intent(this, CalendarWidget::class.java).apply {
//                action = CalendarWidget.ACTION_UPDATE_CALENDAR
//            }
//            sendBroadcast(intent)
//        }
    }

    private fun setOptionsListener(binding: ActivityMainBinding) {
        /////////////////////////
        // General
        /////////////////////////
        openDocumentTreeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
//                    koReaderPath = uri
                    persistAccessPermission(uri)

                    val koReadingStatisticsDBPath = setDBPathOptionAndIcon(uri)
                    // save the statistics db path into preference
                    if (koReadingStatisticsDBPath != null){
                        // Save the koreader statistics db path into the preference
                        val sharedPreferences = getSharedPreferences("calendar_preference", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("reading_statistics_db_path", koReadingStatisticsDBPath.toString())
                        editor.apply()
                    }
                    binding.settingGeneralDbPathContent.text = convertReadablePath(this, uri)
                }
            }
        }

        binding.settingGeneralDbPath.setOnClickListener{ view ->
//            val currentTimestamp = System.currentTimeMillis() / 1000
//            var statisticsHandler = KoReadingStatisticsDBHandler(this, koReadingStatisticsDBPath!!)
//            if (::statisticsDataset.isInitialized) statisticsHandler.retrieveBooksFromPeriod(statisticsHandler.calculateCurrentMonthStartTimestamps(), currentTimestamp)
//            if (::statisticsDataset.isInitialized) statisticsHandler.retrieveDaysFromPeriod(statisticsHandler.calculateCurrentMonthStartTimestamps(), currentTimestamp)

            findKoreaderDB()

//            val intent = Intent(this, CalendarWidget::class.java).apply {
//                action = CalendarWidget.ACTION_UPDATE_CALENDAR
//            }
//            sendBroadcast(intent)
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

    private fun findKoreaderDB() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        openDocumentTreeLauncher.launch(intent)
    }

    fun getFileUriInFolder(folderUri: Uri?, filePath: String): Uri? {
        folderUri?.let {
            val treeDocumentId = DocumentsContract.getTreeDocumentId(folderUri)
            val targetDocumentId = "$treeDocumentId/$filePath"
            return DocumentsContract.buildDocumentUriUsingTree(folderUri, targetDocumentId)
        }
        return null
    }

    fun getStatisticsPath(koreaderPath: Uri): Uri? {
        if (koreaderPath != null) {
            val statisticsRelativePath = getFileUriInFolder(koreaderPath, "settings/statistics.sqlite3")
            val documentFile = DocumentFile.fromSingleUri(this, statisticsRelativePath!!)
            if (documentFile != null && documentFile.exists()) {
                Log.d("DocumentFile", "File exists: ${documentFile.name}")
                return statisticsRelativePath
            } else {
                Log.e("DocumentFile", "File not found: $statisticsRelativePath")
                return null
            }
        }
        return null
    }

    private fun setDBPathOptionAndIcon(koreaderUri: Uri): Uri? {
        val koReadingStatisticsDBPath = getStatisticsPath(koreaderUri)
        if (koReadingStatisticsDBPath != null){
            val drawable: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_check)
            Log.d("calendar", "Binding: ${binding.icDbPathState}")
            binding.icDbPathState.setImageDrawable(drawable)
            Log.d("calendar","drawable set $koReadingStatisticsDBPath")
        }
        else{
            val drawable: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_cross)
            binding.icDbPathState.setImageDrawable(drawable)
        }
        binding.settingGeneralDbPathContent.text = convertReadablePath(this, koreaderUri)
        return koReadingStatisticsDBPath
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

    fun convertReadablePath(context: Context, uri: Uri): String? {
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