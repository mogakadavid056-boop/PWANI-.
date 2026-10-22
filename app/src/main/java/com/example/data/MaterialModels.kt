package com.example.data

data class MaterialItem(
  val id: String,
  val classId: String = "",
  val title: String,
  val description: String,
  val materialType: String,
  val externalUrl: String,
  val storagePath: String = "",
  val fileName: String = "",
  val fileSize: Long = 0L,
  val mimeType: String = "",
  val createdAt: String
) {
  val isFile: Boolean
    get() = storagePath.isNotBlank()

  val formattedFileSize: String
    get() = formatFileSize(fileSize)

  val fileCategory: FileCategory
    get() = detectFileCategory(fileName, mimeType)

  val typeLabel: String
    get() = when (materialType.lowercase()) {
      "notes" -> "Notes"
      "slides" -> "Slides"
      "past_paper" -> "Past Paper"
      "video" -> "Video"
      "link" -> "Link"
      else -> materialType.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

  val formattedDate: String
    get() {
      if (createdAt.isBlank()) return ""
      return try {
        createdAt.take(10)
      } catch (_: Exception) {
        createdAt
      }
    }
}

enum class FileCategory {
  Pdf, Word, PowerPoint, Excel, Image, Text, Generic
}

fun detectFileCategory(fileName: String, mimeType: String): FileCategory {
  val lowerName = fileName.lowercase()
  val lowerMime = mimeType.lowercase()
  return when {
    lowerMime.contains("pdf") || lowerName.endsWith(".pdf") -> FileCategory.Pdf
    lowerMime.contains("word") || lowerMime.contains("officedocument.wordprocessingml") ||
      lowerName.endsWith(".doc") || lowerName.endsWith(".docx") -> FileCategory.Word
    lowerMime.contains("powerpoint") || lowerMime.contains("officedocument.presentationml") ||
      lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx") -> FileCategory.PowerPoint
    lowerMime.contains("excel") || lowerMime.contains("officedocument.spreadsheetml") ||
      lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx") -> FileCategory.Excel
    lowerMime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
      lowerName.endsWith(".png") || lowerName.endsWith(".webp") -> FileCategory.Image
    lowerMime.startsWith("text/") || lowerName.endsWith(".txt") -> FileCategory.Text
    else -> FileCategory.Generic
  }
}

fun formatFileSize(bytes: Long): String {
  if (bytes <= 0) return "0 B"
  val kb = bytes / 1024.0
  val mb = kb / 1024.0
  val gb = mb / 1024.0
  return when {
    gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
    mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
    kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
    else -> "$bytes B"
  }
}
