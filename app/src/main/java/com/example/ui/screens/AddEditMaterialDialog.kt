package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MaterialFileValidator
import com.example.data.MaterialItem
import com.example.ui.components.MaterialFilePickerBox
import com.example.ui.components.MaterialModeSelector
import com.example.ui.components.queryFileInfo
import com.example.ui.theme.*
import java.io.InputStream

@Composable
fun AddEditMaterialDialog(
  initialMaterial: MaterialItem? = null,
  isLoading: Boolean = false,
  uploadProgress: Float? = null,
  onDismiss: () -> Unit,
  onSaveLink: (title: String, description: String, materialType: String, externalUrl: String) -> Unit,
  onSaveUpload: (title: String, description: String, fileName: String, fileSize: Long, mimeType: String, inputStreamProvider: () -> InputStream) -> Unit,
  onSaveEdit: (materialId: String, title: String, description: String) -> Unit
) {
  val context = LocalContext.current
  val isEditing = initialMaterial != null

  var isFileUploadMode by remember { mutableStateOf(false) }
  var title by remember { mutableStateOf(initialMaterial?.title.orEmpty()) }
  var description by remember { mutableStateOf(initialMaterial?.description.orEmpty()) }
  var materialType by remember { mutableStateOf(initialMaterial?.materialType?.lowercase() ?: "notes") }
  var externalUrl by remember { mutableStateOf(initialMaterial?.externalUrl.orEmpty()) }

  var selectedUri by remember { mutableStateOf<Uri?>(null) }
  var selectedFileName by remember { mutableStateOf("") }
  var selectedFileSize by remember { mutableLongStateOf(0L) }
  var selectedMimeType by remember { mutableStateOf("") }
  var validationError by remember { mutableStateOf<String?>(null) }

  val allowedMimes = remember {
    arrayOf(
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
  }

  val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) {
      val (name, size) = queryFileInfo(context, uri)
      val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
      val err = MaterialFileValidator.validate(name, mime, size)
      if (err != null) {
        validationError = err
        selectedUri = null
      } else {
        selectedUri = uri
        selectedFileName = name
        selectedFileSize = size
        selectedMimeType = mime
        validationError = null
        if (title.isBlank()) {
          title = name.substringBeforeLast('.')
        }
      }
    }
  }

  val fieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GoldPrimary, unfocusedBorderColor = DarkBorder,
    focusedLabelColor = GoldPrimary, unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedContainerColor = DarkSurfaceVariant, unfocusedContainerColor = DarkSurfaceVariant
  )

  Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(DarkSurface)
        .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
        .padding(20.dp)
        .verticalScroll(rememberScrollState())
        .testTag("add_edit_material_dialog")
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Description, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isEditing) "Edit Material" else "Add Material",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (!isEditing) {
        MaterialModeSelector(
          isFileUploadMode = isFileUploadMode,
          onSelectLink = { isFileUploadMode = false; validationError = null },
          onSelectUpload = { isFileUploadMode = true; validationError = null }
        )
        Spacer(modifier = Modifier.height(14.dp))
      }

      OutlinedTextField(
        value = title, onValueChange = { title = it; validationError = null },
        label = { Text("Title *") }, singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("material_title_input"), colors = fieldColors
      )
      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = description, onValueChange = { description = it },
        label = { Text("Description (Optional)") }, minLines = 2, maxLines = 4,
        modifier = Modifier.fillMaxWidth().testTag("material_desc_input"), colors = fieldColors
      )
      Spacer(modifier = Modifier.height(12.dp))

      if (!isEditing && !isFileUploadMode) {
        OutlinedTextField(
          value = externalUrl, onValueChange = { externalUrl = it; validationError = null },
          label = { Text("External URL * (e.g. https://...)") }, singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("material_url_input"), colors = fieldColors
        )
      } else if (!isEditing && isFileUploadMode) {
        MaterialFilePickerBox(
          selectedUri = selectedUri,
          fileName = selectedFileName,
          fileSize = selectedFileSize,
          onClick = { filePickerLauncher.launch(allowedMimes) }
        )
      }

      if (uploadProgress != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text("Uploading: ${(uploadProgress * 100).toInt()}%", fontSize = 12.sp, color = GoldLight, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
          progress = { uploadProgress },
          modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
          color = GoldPrimary,
          trackColor = DarkSurfaceVariant
        )
      }

      if (!validationError.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(validationError!!, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.testTag("material_validation_error"))
      }

      Spacer(modifier = Modifier.height(18.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        OutlinedButton(
          onClick = onDismiss, enabled = !isLoading,
          shape = RoundedCornerShape(10.dp), modifier = Modifier.testTag("cancel_material_btn")
        ) {
          Text("Cancel", color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(10.dp))
        val isOnline by com.example.data.NetworkMonitor.isOnline.collectAsState()
        Button(
          onClick = {
            if (!isOnline) {
              com.example.ui.components.OfflineToastHelper.show(context)
              return@Button
            }
            if (title.isBlank()) {
              validationError = "Please enter a title"
              return@Button
            }
            if (isEditing) {
              onSaveEdit(initialMaterial.id, title, description)
            } else if (isFileUploadMode) {
              val uri = selectedUri
              if (uri == null) {
                validationError = "Please choose a file to upload"
                return@Button
              }
              onSaveUpload(title, description, selectedFileName, selectedFileSize, selectedMimeType) {
                context.contentResolver.openInputStream(uri)!!
              }
            } else {
              if (externalUrl.isBlank()) {
                validationError = "Please enter an external URL"
                return@Button
              }
              onSaveLink(title, description, materialType, externalUrl)
            }
          },
          enabled = !isLoading,
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.alpha(if (!isOnline) 0.5f else 1f).testTag("submit_material_btn")
        ) {
          if (isLoading) {
            CircularProgressIndicator(color = DarkTextOnGold, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
          } else {
            Text(if (isEditing) "Save" else "Upload", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
