package com.example.kobowidget

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.github.luben.zstd.Zstd
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

const val TYPE_BB4 = 0
const val TYPE_BB8 = 1
const val TYPE_BB8A = 2
const val TYPE_BBRGB16 = 3
const val TYPE_BBRGB24 = 4
const val TYPE_BBRGB32 = 5

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
    fun printAllBook() : Bitmap? {
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
                if (title == "饮啜尸汁"){
                    Log.d("BookInfo", "Title: $title, Cover Width: $coverWidth," +
                            " Cover Height: $coverHeight, Cover Data Size: ${coverData.size}")
                    val decompressedData: ByteArray = Zstd.decompress(coverData, coverData.size * 10)
                    Log.d("BookInfo", "decompressedData ${decompressedData.size} ${coverStride * coverHeight}")

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
                    return BookInfo(id, Uri.parse(filePath), title, authors, coverWidth, coverHeight, coverBBType, coverStride, coverData)

                } while (cursor.moveToNext())
            }
        }

        return null
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
