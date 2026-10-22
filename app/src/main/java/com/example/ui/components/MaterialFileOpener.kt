package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object MaterialFileOpener {
  fun openOrShareFile(context: Context, file: File, mimeType: String) {
    val cleanMime = mimeType.ifBlank { "*/*" }
    val uri = try {
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (_: Exception) {
      return
    }

    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, cleanMime)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
      context.startActivity(viewIntent)
    } catch (_: Exception) {
      // If no app can open it, offer Share instead
      try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
          type = cleanMime
          putExtra(Intent.EXTRA_STREAM, uri)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share file").apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
      } catch (_: Exception) {}
    }
  }
}
