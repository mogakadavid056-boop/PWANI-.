package com.example.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.AttendanceRegisterDetail
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object AttendanceRegisterActions {

  fun downloadToDownloads(context: Context, detail: AttendanceRegisterDetail): Result<String> {
    return try {
      val pdfFile = AttendanceRegisterPdf.generatePdfFile(context, detail)
      val cleanName = "${detail.registerNo.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "register" }}.pdf"

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
          put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
          put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
          put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
          put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
          ?: return Result.failure(Exception("Failed to create download entry"))

        resolver.openOutputStream(uri)?.use { out ->
          FileInputStream(pdfFile).use { it.copyTo(out) }
        }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
      } else {
        @Suppress("DEPRECATION")
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val destFile = File(downloadsDir, cleanName)
        FileInputStream(pdfFile).use { input ->
          FileOutputStream(destFile).use { output -> input.copyTo(output) }
        }
      }
      Result.success("Saved $cleanName to Downloads")
    } catch (e: Exception) {
      Result.failure(Exception(e.localizedMessage ?: "Failed to download PDF"))
    }
  }

  fun printRegister(context: Context, detail: AttendanceRegisterDetail) {
    try {
      val pdfFile = AttendanceRegisterPdf.generatePdfFile(context, detail)
      val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
      val docName = "${detail.registerNo}.pdf"

      val adapter = object : PrintDocumentAdapter() {
        override fun onLayout(
          oldAttributes: PrintAttributes?,
          newAttributes: PrintAttributes?,
          cancellationSignal: CancellationSignal?,
          callback: LayoutResultCallback?,
          extras: Bundle?
        ) {
          if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
          }
          val info = PrintDocumentInfo.Builder(docName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
          callback?.onLayoutFinished(info, newAttributes != oldAttributes)
        }

        override fun onWrite(
          pages: Array<out PageRange>?,
          destination: ParcelFileDescriptor?,
          cancellationSignal: CancellationSignal?,
          callback: WriteResultCallback?
        ) {
          if (cancellationSignal?.isCanceled == true) {
            callback?.onWriteCancelled()
            return
          }
          try {
            FileInputStream(pdfFile).use { input ->
              FileOutputStream(destination?.fileDescriptor).use { output ->
                input.copyTo(output)
              }
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
          } catch (e: Exception) {
            callback?.onWriteFailed(e.localizedMessage)
          }
        }
      }

      printManager.print(detail.registerNo, adapter, PrintAttributes.Builder().build())
    } catch (_: Exception) {}
  }

  fun shareRegister(context: Context, detail: AttendanceRegisterDetail) {
    try {
      val pdfFile = AttendanceRegisterPdf.generatePdfFile(context, detail)
      val authority = "${context.packageName}.fileprovider"
      val uri = FileProvider.getUriForFile(context, authority, pdfFile)

      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "${detail.registerNo} Attendance Register")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      val chooser = Intent.createChooser(intent, "Share Attendance Register")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    } catch (_: Exception) {}
  }
}
