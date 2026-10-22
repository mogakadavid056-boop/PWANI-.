package com.example.data

import android.content.Context
import java.io.File

object OfflineMaterialsManager {
  const val MAX_STORAGE_BYTES = 300L * 1024L * 1024L // 300 MB

  fun getUserMaterialsDir(context: Context, userId: String): File {
    val safeUid = if (userId.isNotBlank()) userId else "anonymous"
    val dir = File(context.filesDir, "materials/$safeUid")
    if (!dir.exists()) dir.mkdirs()
    return dir
  }

  fun getFileForMaterial(context: Context, userId: String, material: MaterialItem): File {
    val dir = getUserMaterialsDir(context, userId)
    val safeName = MaterialFileValidator.sanitizeFileName(material.fileName.ifBlank { "file" })
    return File(dir, "${material.id}_$safeName")
  }

  fun isDownloaded(context: Context, userId: String, material: MaterialItem): Boolean {
    if (userId.isBlank() || !material.isFile) return false
    val file = getFileForMaterial(context, userId, material)
    return file.exists() && file.length() > 0L
  }

  fun getDownloadedFile(context: Context, userId: String, material: MaterialItem): File? {
    if (userId.isBlank() || !material.isFile) return null
    val file = getFileForMaterial(context, userId, material)
    return if (file.exists() && file.length() > 0L) file else null
  }

  fun getTotalStorageBytes(context: Context, userId: String): Long {
    val dir = getUserMaterialsDir(context, userId)
    val files = dir.listFiles() ?: return 0L
    var sum = 0L
    for (f in files) {
      if (f.isFile) sum += f.length()
    }
    return sum
  }

  fun canDownload(context: Context, userId: String, additionalBytes: Long): Boolean {
    val current = getTotalStorageBytes(context, userId)
    return (current + additionalBytes) <= MAX_STORAGE_BYTES
  }

  fun removeDownload(context: Context, userId: String, material: MaterialItem): Boolean {
    if (userId.isBlank() || !material.isFile) return false
    val file = getFileForMaterial(context, userId, material)
    return if (file.exists()) file.delete() else true
  }

  fun clearAllUserDownloads(context: Context, userId: String) {
    if (userId.isBlank()) return
    try {
      val dir = File(context.filesDir, "materials/$userId")
      if (dir.exists()) dir.deleteRecursively()
    } catch (_: Exception) {}
  }
}
