package com.example.data

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.InputStream

class CountingRequestBody(
  private val inputStreamProvider: () -> InputStream,
  private val totalBytes: Long,
  private val contentType: MediaType?,
  private val onProgress: (Float) -> Unit
) : RequestBody() {

  override fun contentType(): MediaType? = contentType

  override fun contentLength(): Long = totalBytes

  override fun writeTo(sink: BufferedSink) {
    val buffer = ByteArray(8192)
    var uploaded = 0L
    inputStreamProvider().use { input ->
      var read: Int
      while (input.read(buffer).also { read = it } != -1) {
        sink.write(buffer, 0, read)
        uploaded += read
        if (totalBytes > 0) {
          val progress = (uploaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
          onProgress(progress)
        }
      }
    }
    sink.flush()
  }
}

object MaterialFileValidator {
  const val MAX_FILE_SIZE_BYTES = 15L * 1024L * 1024L // 15 MB

  val ALLOWED_MIME_TYPES = setOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/plain",
    "image/jpeg",
    "image/png",
    "image/webp"
  )

  val ALLOWED_EXTENSIONS = setOf(
    "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "jpg", "jpeg", "png", "webp"
  )

  fun sanitizeFileName(name: String): String {
    val clean = name.replace(Regex("[^a-zA-Z0-9.\\-_]"), "_")
    return if (clean.length > 100) clean.take(100) else clean
  }

  fun validate(fileName: String, mimeType: String, sizeBytes: Long): String? {
    if (sizeBytes > MAX_FILE_SIZE_BYTES) {
      return "The selected file is too large. The maximum allowed size is 15 MB."
    }
    val ext = fileName.substringAfterLast('.', "").lowercase()
    val isExtAllowed = ALLOWED_EXTENSIONS.contains(ext)
    val isMimeAllowed = ALLOWED_MIME_TYPES.contains(mimeType.lowercase()) ||
      (mimeType.startsWith("image/") && (ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp"))

    if (!isExtAllowed && !isMimeAllowed) {
      return "File type not allowed. Please choose a PDF, Word, PowerPoint, Excel, text, JPEG, PNG, or WebP file."
    }
    return null
  }
}
