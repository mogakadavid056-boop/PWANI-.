package com.example.admin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminExamResultsPdfExporter
import com.example.admin.ui.AdminMonitorExtraState
import com.example.admin.ui.AdminMonitorExtraViewModel
import com.example.admin.ui.components.AdminEmptyState
import com.example.admin.ui.components.AdminGlassCard
import com.example.admin.ui.components.AdminStatusPill
import com.example.admin.ui.theme.ChampagneGold
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.EmeraldSuccess
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.InfoBlue
import com.example.admin.ui.theme.TextMuted
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun AdminMonitorExamsTab(
  className: String,
  extraState: AdminMonitorExtraState,
  extraVm: AdminMonitorExtraViewModel
) {
  val context = LocalContext.current
  val selected = extraState.selectedExam

  if (selected != null) {
    BackHandler(onBack = extraVm::closeExamDetail)
    val ranked = remember(extraState.examResults, selected.totalMarks) {
      AdminExamResultsPdfExporter.computeRankedEntries(extraState.examResults, selected.totalMarks)
    }

    Column(modifier = Modifier.fillMaxSize().testTag("admin_exam_detail_view")) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = extraVm::closeExamDetail, modifier = Modifier.size(48.dp)) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to exams", tint = ChampagneGold)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(selected.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
          Text("${extraState.examResults.size} submissions • Ranked automatically", fontSize = 12.sp, color = GoldLight)
        }
      }

      if (extraState.examResults.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = { extraVm.exportRankedResultsPdf(context, className, selected) },
          enabled = !extraState.isActionLoading,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold),
          modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("admin_export_ranked_pdf_btn")
        ) {
          Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Export Ranked Results PDF", fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (ranked.isEmpty()) {
        AdminEmptyState(Icons.Default.Quiz, "No Submissions Yet", "Student attempts & security flags will appear here once submitted.")
      } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
          itemsIndexed(ranked, key = { _, e -> e.row.studentId }) { _, entry ->
            val r = entry.row
            AdminGlassCard {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                  AdminStatusPill("#${entry.rank}", if (entry.rank <= 3 && r.score != null) GoldPrimary else InfoBlue)
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(r.fullName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("${r.admissionNumber} • ${r.markingStatus}", fontSize = 12.sp, color = TextSecondary)
                    if (r.late || r.focusLossCount > 0 || r.autoSubmittedReason.isNotBlank()) {
                      Spacer(modifier = Modifier.height(4.dp))
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (r.late) AdminStatusPill("LATE", CrimsonError)
                        if (r.focusLossCount > 0) AdminStatusPill("EXIT (${r.focusLossCount})", CrimsonError)
                        if (r.autoSubmittedReason.isNotBlank()) AdminStatusPill("AUTO", GoldLight)
                      }
                    }
                  }
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = r.score?.let { String.format(Locale.US, "%.1f / %.1f", it, r.total.coerceAtLeast(1.0)) } ?: "Pending",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (r.score != null) EmeraldSuccess else TextMuted
                  )
                  Text(
                    text = if (r.score != null) "${entry.percentage.toInt()}% (${entry.gradeLabel})" else "Pending",
                    fontSize = 12.sp,
                    color = ChampagneGold
                  )
                }
              }
            }
          }
        }
      }
    }
    return
  }

  Column(modifier = Modifier.fillMaxSize().testTag("admin_monitor_exams_tab")) {
    Text("Exams & Challenges (${extraState.exams.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldLight)
    Spacer(modifier = Modifier.height(10.dp))
    if (extraState.exams.isEmpty()) {
      AdminEmptyState(Icons.Default.Quiz, "No Exams or Challenges", "Assessments published by lecturers will appear here for ranking & monitoring.")
    } else {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        itemsIndexed(extraState.exams, key = { _, q -> q.id }) { _, quiz ->
          AdminGlassCard(onClick = { extraVm.openExam(quiz) }) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text(quiz.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                val info = listOfNotNull(
                  quiz.unitCode.takeIf { it.isNotBlank() },
                  "${quiz.questionCount} Qs",
                  "${quiz.attemptsCount} submissions"
                ).joinToString(" • ")
                Text(info, fontSize = 12.sp, color = TextSecondary)
              }
              AdminStatusPill(
                text = if (quiz.isExam) "EXAM" else quiz.status.uppercase(),
                color = if (quiz.isExam) GoldPrimary else InfoBlue
              )
            }
            if (quiz.oneWayNavigation || quiz.noCopyPaste) {
              Spacer(modifier = Modifier.height(6.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (quiz.oneWayNavigation) AdminStatusPill("ONE-WAY", InfoBlue)
                if (quiz.noCopyPaste) AdminStatusPill("COPY BLOCKED", EmeraldSuccess)
              }
            }
          }
        }
      }
    }
  }
}
