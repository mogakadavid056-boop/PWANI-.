package com.example.admin.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.admin.data.AdminCourseUnit
import com.example.admin.data.AdminFileValidator
import com.example.admin.data.AdminMaterialItem
import com.example.admin.ui.components.AdminConfirmDialog
import com.example.admin.ui.components.AdminEmptyState
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.AdminSurface
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.InfoBlue
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary
import java.io.File
import java.io.InputStream

private data class PickedDocMeta(val uri: Uri, val name: String, val size: Long, val mime: String)

@Composable
fun AdminMonitorMaterialsTab(
  units: List<AdminCourseUnit>,
  materials: List<AdminMaterialItem>,
  uploadProgress: Float?,
  isActionLoading: Boolean,
  onAddLink: (String, String, String, String, String) -> Unit,
  onUploadFile: (String, String, String, String, Long, String, () -> InputStream) -> Unit,
  onUpdateMaterial: (String, String, String) -> Unit,
  onDeleteMaterial: (AdminMaterialItem) -> Unit,
  onDownloadMaterial: (AdminMaterialItem, File, (File, String) -> Unit) -> Unit
) {
  val context = LocalContext.current
  val approvedUnits = remember(units) { units.filter { it.isApproved } }
  var filterUnitId by remember { mutableStateOf<String?>(null) }
  var showAddLink by remember { mutableStateOf(false) }
  var pickedDoc by remember { mutableStateOf<PickedDocMeta?>(null) }
  var editing by remember { mutableStateOf<AdminMaterialItem?>(null) }
  var deleteTarget by remember { mutableStateOf<AdminMaterialItem?>(null) }

  val docLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) {
      val meta = readDocumentMeta(context, uri)
      val err = AdminFileValidator.validate(meta.name, meta.mime, meta.size)
      if (err != null) Toast.makeText(context, err, Toast.LENGTH_LONG).show()
      else pickedDoc = meta
    }
  }

  val filtered = remember(materials, filterUnitId) {
    if (filterUnitId == null) materials else materials.filter { it.courseId == filterUnitId }
  }
  val unitMap = remember(units) { units.associateBy { it.courseId } }
  val grouped = remember(filtered) { filtered.groupBy { it.courseId } }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("Materials (${filtered.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldLight)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = { showAddLink = true },
          enabled = !isActionLoading && approvedUnits.isNotEmpty(),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Default.Link, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Link", fontSize = 12.sp, color = GoldLight)
        }
        Button(
          onClick = { docLauncher.launch(arrayOf("*/*")) },
          enabled = !isActionLoading && approvedUnits.isNotEmpty(),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
        ) {
          Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Upload", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    if (approvedUnits.isEmpty()) {
      Spacer(modifier = Modifier.height(6.dp))
      Text("Register or approve a unit first before adding materials.", fontSize = 12.sp, color = TextMuted)
    }

    uploadProgress?.let { p ->
      Spacer(modifier = Modifier.height(8.dp))
      LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
    }

    if (approvedUnits.isNotEmpty()) {
      Spacer(modifier = Modifier.height(8.dp))
      Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
          selected = filterUnitId == null,
          onClick = { filterUnitId = null },
          label = { Text("All Units", fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary, selectedLabelColor = DarkTextOnGold, containerColor = AdminSurface, labelColor = TextSecondary)
        )
        approvedUnits.forEach { u ->
          FilterChip(
            selected = filterUnitId == u.courseId,
            onClick = { filterUnitId = u.courseId },
            label = { Text(u.code, fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary, selectedLabelColor = DarkTextOnGold, containerColor = AdminSurface, labelColor = TextSecondary)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    if (filtered.isEmpty()) {
      AdminEmptyState(Icons.Default.Description, "No materials yet", "Upload notes/slides or attach an external link.")
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        grouped.forEach { (courseId, list) ->
          val header = unitMap[courseId]?.displayLabel ?: "General / Other"
          item(key = "hdr_$courseId") {
            Text(header, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
          }
          items(list, key = { it.id }) { mat ->
            AdminGlassCard(onClick = {
              if (mat.isFile) {
                onDownloadMaterial(mat, context.cacheDir) { file, mime -> openLocalFile(context, file, mime) }
              } else if (mat.externalUrl.isNotBlank()) {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mat.externalUrl))) }
              }
            }) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                  AdminStatusPill(mat.materialType.uppercase(), InfoBlue)
                  Spacer(modifier = Modifier.height(5.dp))
                  Text(mat.title.ifBlank { mat.fileName.ifBlank { "Material" } }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                  if (mat.description.isNotBlank()) Text(mat.description, fontSize = 12.sp, color = TextSecondary)
                  val meta = if (mat.isFile) "${mat.fileName} (${formatKb(mat.fileSize)})" else mat.externalUrl
                  if (meta.isNotBlank()) Text(meta, fontSize = 11.sp, color = TextMuted, maxLines = 1)
                }
                Row {
                  IconButton(onClick = { editing = mat }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit material", tint = GoldLight, modifier = Modifier.size(18.dp))
                  }
                  IconButton(onClick = { deleteTarget = mat }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete material", tint = CrimsonError, modifier = Modifier.size(18.dp))
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddLink && approvedUnits.isNotEmpty()) {
    AddLinkMaterialDialog(approvedUnits, isActionLoading, { cid, t, d, type, url -> onAddLink(cid, t, d, type, url); showAddLink = false }) { showAddLink = false }
  }
  pickedDoc?.let { doc ->
    if (approvedUnits.isNotEmpty()) {
      UploadFileMaterialDialog(doc.name, approvedUnits, isActionLoading, { cid, title, desc ->
        onUploadFile(cid, title, desc, doc.name, doc.size, doc.mime) { context.contentResolver.openInputStream(doc.uri)!! }
        pickedDoc = null
      }) { pickedDoc = null }
    }
  }
  editing?.let { mat ->
    EditMaterialDialog(mat, isActionLoading, { t, d -> onUpdateMaterial(mat.id, t, d); editing = null }) { editing = null }
  }
  deleteTarget?.let { mat ->
    AdminConfirmDialog("Delete Material?", "Delete \"${mat.title}\" permanently?", "Delete", isDestructive = true, isActionLoading = isActionLoading,
      onConfirm = { onDeleteMaterial(mat); deleteTarget = null }, onDismiss = { deleteTarget = null })
  }
}

private fun readDocumentMeta(context: Context, uri: Uri): PickedDocMeta {
  var name = "document"
  var size = 0L
  context.contentResolver.query(uri, null, null, null, null)?.use { c ->
    val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
    if (c.moveToFirst()) {
      if (nameIdx >= 0) name = c.getString(nameIdx) ?: name
      if (sizeIdx >= 0) size = c.getLong(sizeIdx)
    }
  }
  val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
  return PickedDocMeta(uri, name, size, mime)
}

private fun openLocalFile(context: Context, file: File, mime: String) {
  val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  val intent = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(uri, mime.ifBlank { "*/*" })
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  runCatching { context.startActivity(Intent.createChooser(intent, "Open Material")) }
    .onFailure { Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show() }
}

private fun formatKb(bytes: Long): String = if (bytes >= 1024L * 1024L) "${bytes / (1024L * 1024L)} MB" else "${(bytes / 1024L).coerceAtLeast(1)} KB"
