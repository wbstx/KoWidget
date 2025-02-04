package com.example.kobowidget

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.github.luben.zstd.Zstd
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Calendar

const val TYPE_BB4 = 0
const val TYPE_BB8 = 1
const val TYPE_BB8A = 2
const val TYPE_BBRGB16 = 3
const val TYPE_BBRGB24 = 4
const val TYPE_BBRGB32 = 5

private const val REQUEST_CODE = 1001

class KoReaderBookInfoDBHandler (
    private var context: Context,
    dbPath: Uri
) {
    private lateinit var bookInfoDataset: SQLiteDatabase

    init {
        readSQLiteDatabase(dbPath)
    }

    private fun readSQLiteDatabase(uri: Uri) {
        try {
            val inputStream = this.context.contentResolver.openInputStream(uri)
            val cacheDir = this.context.cacheDir
            val tempFile = File(cacheDir, "temp_bookinfo.sqlite")
            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            bookInfoDataset = SQLiteDatabase.openDatabase(
                tempFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("Range")
    fun printAllBook(): Bitmap? {
        val sql = """
                SELECT * FROM bookinfo
            """.trimIndent()

        val cursor = bookInfoDataset.rawQuery(sql, null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex("bcid"))
                val title = cursor.getString(cursor.getColumnIndex("title"))
                val coverWidth = cursor.getInt(cursor.getColumnIndex("cover_w"))
                val coverHeight = cursor.getInt(cursor.getColumnIndex("cover_h"))
                val coverBBType = cursor.getInt(cursor.getColumnIndex("cover_bb_type"))
                val coverStride = cursor.getInt(cursor.getColumnIndex("cover_bb_stride"))
                val coverData = cursor.getBlob(cursor.getColumnIndex("cover_bb_data"))

                Log.d("BookInfo", "ID: $id, Title: $title, Cover Width: $coverWidth")
                if (title == "饮啜尸汁") {
                    Log.d(
                        "BookInfo", "Title: $title, Cover Width: $coverWidth," +
                                " Cover Height: $coverHeight, Cover Data Size: ${coverData.size}"
                    )
                    val decompressedData: ByteArray =
                        Zstd.decompress(coverData, coverData.size * 10)
                    Log.d(
                        "BookInfo",
                        "decompressedData ${decompressedData.size} ${coverStride * coverHeight}"
                    )

//                    if (decompressedData != null){
////                        val bitmap: Bitmap = BitmapFactory.decodeByteArray(decompressedData, 0, decompressedData.size)
//                        val bitmap = byteArrayToBitmapRGB(decompressedData, coverWidth, coverHeight)
//                        return bitmap
//                    }

                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        return null
    }

    @SuppressLint("Range")
    fun getBookInfoByFilename(filePath: String): BookInfo? {
        val sql = """
                SELECT * FROM bookinfo
                WHERE directory = ? AND filename = ?
            """.trimIndent()

        val file = File(filePath)
        val directory = file.parent?.plus('/')
        val filename = file.name

        val cursor = bookInfoDataset.rawQuery(sql, arrayOf(directory, filename))

        Log.d("BookInfo", "Finding Book!: $directory, $filename")
        val bookfile = File(directory + filename)
        val uri = getDocumentUriInFolder(
            Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ABooks"),
            file.name
        )
        Log.d("BookInfo", "$uri")
        partialMD5(uri)?.let {
            Log.d(
                "BookInfo",
                "MD5: $it, golden: 50ea215e2749d6e22e25f582771592b6"
            )
        }

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndex("bcid"))
                    val title = cursor.getString(cursor.getColumnIndex("title"))
                    val authors = cursor.getString(cursor.getColumnIndex("authors"))
                    val coverWidth = cursor.getInt(cursor.getColumnIndex("cover_w"))
                    val coverHeight = cursor.getInt(cursor.getColumnIndex("cover_h"))
                    val coverBBType = cursor.getInt(cursor.getColumnIndex("cover_bb_type"))
                    val coverStride = cursor.getInt(cursor.getColumnIndex("cover_bb_stride"))
                    val coverData = cursor.getBlob(cursor.getColumnIndex("cover_bb_data"))

                    Log.d("BookInfo", "Find Book!: ID: $id, Title: $title")
                    // TODO: check None, especially book without cover
                    return BookInfo(
                        id,
                        Uri.parse(filePath),
                        title,
                        authors,
                        coverWidth,
                        coverHeight,
                        coverBBType,
                        coverStride,
                        coverData
                    )

                } while (cursor.moveToNext())
            }
        }

        return null
    }

    private fun getDocumentUriInFolder(folderUri: Uri?, filePath: String): Uri? {
        folderUri?.let {
            val treeDocumentId = DocumentsContract.getTreeDocumentId(folderUri)
            val targetDocumentId = "$treeDocumentId/$filePath"
            val targetDocumentPath =
                DocumentsContract.buildDocumentUriUsingTree(folderUri, targetDocumentId)
            Log.d("DocumentFile", "$folderUri, $filePath")

            val documentFile = DocumentFile.fromSingleUri(this.context, targetDocumentPath)
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

    private fun partialMD5(uri: Uri?): String? {
        if (uri == null) return null

        try {
            val contentResolver: ContentResolver = this.context.contentResolver
            var fileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: return null
//            var fileInputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val md = MessageDigest.getInstance("MD5")
            val step = 1024
            val size = 1024
            val buffer = ByteArray(size)
            FileInputStream(fileDescriptor.fileDescriptor).use { fileInputStream ->
                val channel = fileInputStream.channel
                for (i in -1..10) {
                    val position = (step shl (2 * i)).toLong()
                    if (position >= channel.size()) continue
                    try {
                        channel.position(position)
                        val bytesRead = channel.read(ByteBuffer.wrap(buffer, 0, size))
                        if (bytesRead == -1) break
                        md.update(buffer, 0, bytesRead)

//                        var totalRead = 0
//                        while (totalRead < size) {
//                            val bytesRead = channel.read(ByteBuffer.wrap(buffer, totalRead, size - totalRead))
//                            if (bytesRead == -1) break
//                            totalRead += bytesRead
//                        }
//                        if (totalRead > 0) {
//                            md.update(buffer, 0, totalRead)
//                        }

                    } catch (e: IOException) {
                        break
                    }
                }
            }

//            for (i in -1..10) {
//                position = (step shl (2 * i)).toLong()
//
//                fileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: return null
//                fileInputStream = FileInputStream(fileDescriptor.fileDescriptor)
//                fileInputStream.skip(position)
//
//                val sample = ByteArray(size)
//                val bytesRead = fileInputStream.read(sample)
//
//                fileInputStream.close()
//
//                if (bytesRead == -1) {
//                    break
//                }
//
//                md.update(sample, 0, bytesRead)
//
//            }

            val digest = md.digest()
            return digest.joinToString("") { "%02x".format(it) }

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

    }
}

fun byteArrayToBitmapRGB(data: ByteArray, width: Int, height: Int, coverType: Int, coverStride: Int): Bitmap? {
    if (coverType != TYPE_BBRGB24 && coverType != TYPE_BBRGB32) {
        return null
    }

    Log.d("Bookinfo", "Type is ${coverType}")

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    var index = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (coverType == TYPE_BB8) {
                val c = data[index].toInt() and 0xFF

                val color = Color.rgb(c, c, c)
                bitmap.setPixel(x, y, color)
                bitmap.setPixel(x, y, color)

                index != 1
            }
            if (coverType == TYPE_BBRGB24) {
                val r = data[index].toInt() and 0xFF
                val g = data[index + 1].toInt() and 0xFF
                val b = data[index + 2].toInt() and 0xFF

                val color = Color.rgb(r, g, b)
                bitmap.setPixel(x, y, color)

                index += 3
            }
            if (coverType == TYPE_BBRGB32) {
                val r = data[index].toInt() and 0xFF
                val g = data[index + 1].toInt() and 0xFF
                val b = data[index + 2].toInt() and 0xFF
                val a = data[index + 3].toInt() and 0xFF

                val color = Color.argb(a, r, g, b)
                bitmap.setPixel(x, y, color)

                index += 4
            }
        }
    }

    return bitmap
}

class BookInfo (
    val id: Int,
    val fileUri: Uri,
    val title: String,
    val authors: String,
    private val coverWidth: Int,
    private val coverHeight: Int,
    private val coverBBType: Int,
    private val coverStride: Int,
    private val coverData: ByteArray
) {
    fun getCoverBitmap(): Bitmap? {
        val decompressedData: ByteArray = Zstd.decompress(coverData, coverData.size * 10)
        if (decompressedData.size != coverStride * coverHeight) {
            return null
        }
        return byteArrayToBitmapRGB(decompressedData, coverWidth, coverHeight, coverBBType, coverStride)
    }
}
