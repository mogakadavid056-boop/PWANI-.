package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceRegisterDetail
import com.example.data.AttendanceRegisterRowItem
import com.example.ui.theme.*
import com.example.util.JsonHelper
import com.example.util.SignatureBitmapHelper

@Composable
fun RegisterStatusChip(status: String) {
  val (color, bg) = when (status.lowercase()) {
    "open" -> Pair(GoldPrimary, GoldPrimary.copy(alpha = 0.15f))
    "closed" -> Pair(SuccessGreen, SuccessGreen.copy(alpha = 0.15f))
    else -> Pair(ErrorRed, ErrorRed.copy(alpha = 0.15f))
  }
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(bg)
      .border(1.dp, color, RoundedCornerShape(12.dp))
      .padding(horizontal = 10.dp, vertical = 4.dp)
      .testTag("register_status_chip")
  ) {
    Text(status.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
  }
}

@Composable
fun RegisterSignatureThumbnail(
  base64Signature: String?,
  modifier: Modifier = Modifier
) {
  val bmp = remember(base64Signature) { SignatureBitmapHelper.decodeTrimmedNavyBitmap(base64Signature) }
  if (bmp != null) {
    Box(
      modifier = modifier
        .clip(RoundedCornerShape(6.dp))
        .background(Color.White)
        .border(0.8.dp, DarkBorder, RoundedCornerShape(6.dp))
        .padding(4.dp),
      contentAlignment = Alignment.Center
    ) {
      Image(
        bitmap = bmp.asImageBitmap(),
        contentDescription = "Signature",
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(Color(SignatureBitmapHelper.NAVY_INK_COLOR)),
        modifier = Modifier.fillMaxSize()
      )
    }
  } else if (!base64Signature.isNullOrBlank() && !base64Signature.equals("null", true)) {
    Text("Signed", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
  }
}

@Composable
fun RegisterInfoCard(
  d: AttendanceRegisterDetail,
  canEditDetails: Boolean = false,
  onEditDetails: () -> Unit = {}
) {
  val courseDisplay = listOfNotNull(
    d.courseName?.takeIf { it.isNotBlank() && !it.equals("null", true) },
    d.courseCode?.takeIf { it.isNotBlank() && !it.equals("null", true) }?.let { "($it)" }
  ).joinToString(" ").ifBlank { "-" }

  ClassRepCard(modifier = Modifier.fillMaxWidth().testTag("register_info_card")) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      if (canEditDetails) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("SESSION DETAILS", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = GoldLight)
          TextButton(onClick = onEditDetails, modifier = Modifier.testTag("info_edit_details_btn")) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Edit details", fontSize = 14.sp, color = GoldPrimary, maxLines = 1, softWrap = false)
          }
        }
      }
      InfoRow("Course", courseDisplay)
      InfoRow("Lecturer", JsonHelper.orDash(d.lecturerName))
      InfoRow("Class", JsonHelper.orDash(d.className))
      InfoRow("Date", JsonHelper.orDash(d.dateText))
      InfoRow("Time", JsonHelper.orDash(d.timeText))
      InfoRow("Venue", JsonHelper.orDash(d.venue))
      InfoRow("Attendance", "${d.presentCount} of ${d.total} present")
    }
  }
}

@Composable
private fun InfoRow(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, fontSize = 12.5.sp, color = TextSecondary, modifier = Modifier.padding(end = 12.dp))
    Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
  }
}

@Composable
fun RegisterTablePreview(
  rows: List<AttendanceRegisterRowItem>,
  canEditRecords: Boolean,
  showSignatures: Boolean,
  onCorrectRow: (AttendanceRegisterRowItem) -> Unit,
  onMarkPresentRow: (AttendanceRegisterRowItem) -> Unit
) {
  ClassRepCard(modifier = Modifier.fillMaxWidth().testTag("register_table_preview")) {
    Column {
      Text("ATTENDANCE RECORDS (${rows.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldLight)
      Spacer(modifier = Modifier.height(8.dp))
      rows.forEachIndexed { i, row ->
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${row.n}.", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(JsonHelper.orDash(row.fullName), fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (row.isPrefect) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(GoldPrimary).padding(horizontal = 6.dp, vertical = 1.dp)
                  ) { Text("Prefect", fontSize = 10.sp, color = DarkTextOnGold, fontWeight = FontWeight.Bold) }
                }
              }
              if (row.present && row.manual) {
                Text("(marked by staff)", fontSize = 11.sp, color = GoldLight)
              }
              val adm = JsonHelper.orDash(row.admissionNumber)
              val tIn = if (row.present) JsonHelper.orDash(row.timeInText) else "-"
              val tOut = if (row.present) JsonHelper.orDash(row.timeOutText) else "-"
              Text("$adm • In: $tIn • Out: $tOut", fontSize = 12.sp, color = TextSecondary)
            }

            if (row.present) {
              if (showSignatures && !row.signature.isNullOrBlank()) {
                RegisterSignatureThumbnail(
                  base64Signature = row.signature,
                  modifier = Modifier.width(68.dp).height(30.dp)
                )
              } else {
                Text("Present", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
              }
              if (canEditRecords) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                  onClick = { onCorrectRow(row) },
                  modifier = Modifier.size(36.dp).testTag("correct_time_row_${row.studentId}")
                ) {
                  Icon(Icons.Default.Edit, contentDescription = "Correct time", tint = GoldLight, modifier = Modifier.size(17.dp))
                }
              }
            } else {
              Text("Absent", fontSize = 12.sp, color = ErrorRed, fontWeight = FontWeight.SemiBold)
            }
          }

          if (!row.present && canEditRecords) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
              OutlinedButton(
                onClick = { onMarkPresentRow(row) },
                modifier = Modifier.height(36.dp).testTag("mark_present_btn_${row.studentId}"),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, GoldPrimary),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface, contentColor = GoldLight),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
              ) {
                Text("Mark present", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldLight, maxLines = 1, softWrap = false)
              }
            }
          }
        }
        if (i < rows.size - 1) HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
      }
    }
  }
}

@Composable
fun RegisterSignaturesSection(
  detail: AttendanceRegisterDetail,
  onSignPrefect: () -> Unit,
  onSignLecturer: () -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
    SignerCard(
      title = "Class Prefect",
      name = JsonHelper.orDash(detail.prefectName),
      signature = detail.prefectSignature,
      signedText = detail.prefectSignedText,
      canSign = detail.permissions.canSignPrefect,
      buttonLabel = "Sign as prefect",
      boxTag = "prefect_signature_box",
      btnTag = "sign_as_prefect_btn",
      onSign = onSignPrefect
    )
    SignerCard(
      title = "Lecturer in charge",
      name = JsonHelper.orDash(detail.lecturerName),
      signature = detail.lecturerSignature,
      signedText = detail.lecturerSignedText,
      canSign = detail.permissions.canSignLecturer,
      buttonLabel = "Sign as lecturer",
      boxTag = "lecturer_signature_box",
      btnTag = "sign_as_lecturer_btn",
      onSign = onSignLecturer
    )
  }
}

@Composable
private fun SignerCard(
  title: String,
  name: String,
  signature: String?,
  signedText: String,
  canSign: Boolean,
  buttonLabel: String,
  boxTag: String,
  btnTag: String,
  onSign: () -> Unit
) {
  ClassRepCard(modifier = Modifier.fillMaxWidth().testTag(boxTag)) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldLight)
      Text(name, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
      if (!signature.isNullOrBlank() && !signature.equals("null", true)) {
        RegisterSignatureThumbnail(
          base64Signature = signature,
          modifier = Modifier.fillMaxWidth().height(64.dp)
        )
      }
      val cleanSigned = signedText.trim().takeIf { it.isNotBlank() && !it.equals("null", true) }
      Text(
        text = if (cleanSigned != null) "Signed: $cleanSigned" else "Not signed yet",
        fontSize = 12.sp,
        color = if (cleanSigned != null) SuccessGreen else TextSecondary
      )
      if (canSign) {
        Button(
          onClick = onSign,
          modifier = Modifier.fillMaxWidth().height(42.dp).testTag(btnTag),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
        ) {
          Text(buttonLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkTextOnGold, maxLines = 1, softWrap = false)
        }
      }
    }
  }
}
