package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PdfPrintHelper {
  fun printPdfFile(context: Context, file: File, jobName: String): Boolean {
    if (!file.exists()) return false
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return false
    val adapter = object : PrintDocumentAdapter() {
      override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
      ) {
        if (cancellationSignal?.isCanceled == true) {
          callback.onLayoutCancelled()
          return
        }
        val info = PrintDocumentInfo.Builder(file.name.ifBlank { "marked_paper.pdf" })
          .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
          .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
          .build()
        callback.onLayoutFinished(info, true)
      }

      override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
      ) {
        try {
          FileInputStream(file).use { input ->
            FileOutputStream(destination.fileDescriptor).use { output ->
              val buffer = ByteArray(8192)
              var read: Int
              while (input.read(buffer).also { read = it } != -1) {
                if (cancellationSignal?.isCanceled == true) {
                  callback.onWriteCancelled()
                  return
                }
                output.write(buffer, 0, read)
              }
            }
          }
          callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
          callback.onWriteFailed(e.message)
        }
      }
    }
    return runCatching {
      printManager.print(jobName.ifBlank { "ClassRep Marked Paper" }, adapter, null)
    }.isSuccess
  }

  fun sharePdfFile(context: Context, file: File, title: String = "Marked Exam Paper") {
    if (!file.exists()) return
    val uri = runCatching {
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull() ?: return

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, title)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(shareIntent, "Share Marked Paper").apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(chooser) }
  }
}
