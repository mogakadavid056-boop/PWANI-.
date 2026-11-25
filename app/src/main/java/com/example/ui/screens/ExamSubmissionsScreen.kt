package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChallengeItem
import com.example.data.ExamFlagItem
import com.example.data.ExamSubmissionRow
import com.example.data.MarkingStatus
import com.example.data.formatMarks
import com.example.notifications.NotificationTimeFormatter
import com.example.ui.SubmissionFilter
import com.example.ui.SubmissionSortOrder
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.FriendlyErrorSnackbar
import com.example.ui.components.MetallicGoldText
import com.example.ui.components.RoyalCard
import com.example.ui.components.royalOutlinedTextFieldColors
import com.example.ui.theme.ButtonRadius
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.CardTitleStyle
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.FieldLabelStyle
import com.example.ui.theme.Lavender
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.Midnight
import com.example.ui.theme.NavLabelStyle
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.RoyalSurfaceHigh
import com.example.ui.theme.ScreenTitleStyle
import com.example.ui.theme.SuccessColor

@Composable
fun ExamSubmissionsScreen(
  quiz: ChallengeItem,
  allSubmissions: List<ExamSubmissionRow>,
  displayedSubmissions: List<ExamSubmissionRow>,
  selectedFilter: SubmissionFilter,
  selectedSortOrder: SubmissionSortOrder,
  waitingFirst: Boolean,
  searchQuery: String,
  isLoading: Boolean,
  examFlags: List<ExamFlagItem> = emptyList(),
  readyToBatchReturnCount: Int = 0,
  isBatchReleasing: Boolean = false,
  batchReleaseDone: Int = 0,
  batchReleaseTotal: Int = 0,
  isLoadingPdf: Boolean = false,
  errorMessage: String?,
  successMessage: String? = null,
  onSelectFilter: (SubmissionFilter) -> Unit,
  onSelectSortOrder: (SubmissionSortOrder) -> Unit,
  onToggleWaitingFirst: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onRefresh: () -> Unit,
  onOpenStudentPaper: (ExamSubmissionRow) -> Unit,
  onReleaseAllReady: () -> Unit = {},
  onExportRankedPdf: () -> Unit = {},
  onDismissError: () -> Unit,
  onDismissSuccess: () -> Unit = {},
  onBack: () -> Unit
) {
  BackHandler(onBack = onBack)

  var batchSwitchChecked by remember { mutableStateOf(false) }
  var showBatchConfirm by remember { mutableStateOf(false) }

  val waitingCount = allSubmissions.count { it.markingStatus == MarkingStatus.PENDING }
  val markingCount = allSubmissions.count { it.markingStatus == MarkingStatus.MARKING }
  val returnedCount = allSubmissions.count { it.markingStatus == MarkingStatus.RELEASED }

  Box(modifier = Modifier.fillMaxSize().testTag("exam_submissions_screen")) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("submissions_back_btn")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampagneGold)
          }
          Spacer(modifier = Modifier.width(6.dp))
          MetallicGoldText("Mark Papers", style = ScreenTitleStyle)
        }
        IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp).testTag("submissions_refresh_btn")) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh submissions", tint = ChampagneGold)
        }
      }

      RoyalCard(isHero = true) {
        Text(quiz.title, style = CardTitleStyle)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text("$waitingCount waiting", style = FieldLabelStyle.copy(color = ChampagneGold))
          Text("$markingCount in progress", style = FieldLabelStyle.copy(color = ElectricViolet))
          Text("$returnedCount returned", style = FieldLabelStyle.copy(color = SuccessColor))
        }
        if (allSubmissions.isNotEmpty()) {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedButton(
            onClick = onExportRankedPdf,
            enabled = !isLoadingPdf && !isBatchReleasing,
            shape = RoundedCornerShape(ButtonRadius),
            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("submissions_ranked_pdf_btn")
          ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              if (isLoadingPdf) "Generating Ranked PDF..." else "Export Ranked Results PDF (Staff Only)",
              style = ButtonTextStyle.copy(fontSize = 13.5.sp, color = ChampagneGold)
            )
          }
        }
      }

      if (examFlags.isNotEmpty()) {
        RoyalCard(borderColor = ErrorColor.copy(alpha = 0.55f)) {
          Text(
            text = "Integrity Flags (${examFlags.size})",
            style = CardTitleStyle.copy(fontSize = 15.sp, color = ErrorColor),
            modifier = Modifier.testTag("exam_flags_card")
          )
          examFlags.take(8).forEach { flag ->
            val tags = buildList {
              if (flag.exitViolations > 0) add("Exits: ${flag.exitViolations}")
              if (flag.autoSubmitted) {
                val r = flag.autoSubmitReason.ifBlank { "auto" }
                add("Auto ($r)")
              }
              if (flag.late) add("Late")
            }.joinToString(" • ")
            Text(
              text = "${flag.fullName} (${flag.admissionNumber}) — $tags",
              style = CaptionStyle.copy(color = Lavender)
            )
          }
          if (examFlags.size > 8) {
            Text("+${examFlags.size - 8} more flagged submission(s)", style = CaptionStyle.copy(color = ChampagneGold))
          }
        }
      }

      if (allSubmissions.isNotEmpty()) {
        RoyalCard(borderColor = ChampagneGold.copy(alpha = 0.45f)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text("Return All Marked Papers at Once", style = CardTitleStyle.copy(fontSize = 16.sp, color = ChampagneGold))
              Text(
                text = if (readyToBatchReturnCount > 0) {
                  "$readyToBatchReturnCount fully marked paper(s) ready for bulk PDF generation & return"
                } else {
                  "Mark & save written questions first; switch ON to return all ready papers together"
                },
                style = CaptionStyle
              )
            }
            Switch(
              checked = batchSwitchChecked || isBatchReleasing,
              enabled = !isBatchReleasing && readyToBatchReturnCount > 0,
              onCheckedChange = { checked ->
                batchSwitchChecked = checked
                if (checked && readyToBatchReturnCount > 0) {
                  showBatchConfirm = true
                }
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Midnight,
                checkedTrackColor = MetallicGold,
                uncheckedThumbColor = Lavender,
                uncheckedTrackColor = RoyalSurfaceHigh
              ),
              modifier = Modifier.testTag("batch_return_switch")
            )
          }
          if (batchSwitchChecked && !isBatchReleasing && readyToBatchReturnCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = { showBatchConfirm = true },
              shape = RoundedCornerShape(ButtonRadius),
              colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Midnight),
              modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).testTag("batch_return_now_btn")
            ) {
              Icon(Icons.Default.DoneAll, contentDescription = null, tint = Midnight, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Return $readyToBatchReturnCount Paper(s) Now", style = ButtonTextStyle.copy(color = Midnight))
            }
          }
          if (isBatchReleasing) {
            Spacer(modifier = Modifier.height(8.dp))
            val total = batchReleaseTotal.coerceAtLeast(1)
            val progress = (batchReleaseDone.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
              progress = { progress },
              color = MetallicGold,
              trackColor = RoyalSurfaceHigh,
              modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              "Returning marked PDFs: $batchReleaseDone / $total...",
              style = CaptionStyle.copy(color = ChampagneGold)
            )
          }
        }
      }

      if (isLoading && allSubmissions.isNotEmpty()) {
        LinearProgressIndicator(
          color = MetallicGold,
          trackColor = RoyalSurfaceHigh,
          modifier = Modifier.fillMaxWidth()
        )
      }

      if (!successMessage.isNullOrBlank()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SuccessColor.copy(alpha = 0.16f))
            .border(1.dp, SuccessColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(successMessage, style = CaptionStyle.copy(color = SuccessColor), modifier = Modifier.weight(1f))
          IconButton(onClick = onDismissSuccess, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = SuccessColor, modifier = Modifier.size(16.dp))
          }
        }
      }

      if (!errorMessage.isNullOrBlank()) {
        FriendlyErrorSnackbar(errorMessage = errorMessage, onDismiss = onDismissError)
      }

      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        singleLine = true,
        placeholder = { Text("Search name or admission no...", style = CaptionStyle) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ChampagneGold) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(48.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Lavender)
            }
          }
        },
        colors = royalOutlinedTextFieldColors(),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("submissions_search_input")
      )

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        SubmissionFilter.entries.forEach { filter ->
          val selected = selectedFilter == filter
          FilterChip(
            selected = selected,
            onClick = { onSelectFilter(filter) },
            label = { Text(filter.label, style = NavLabelStyle) },
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("sub_filter_${filter.name.lowercase()}"),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = selected,
              borderColor = DarkBorder,
              selectedBorderColor = ChampagneGold
            ),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = ChampagneGold,
              selectedLabelColor = Midnight,
              containerColor = RoyalSurfaceHigh,
              labelColor = Lavender
            )
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort order", tint = ChampagneGold, modifier = Modifier.size(18.dp))
        SubmissionSortOrder.entries.forEach { order ->
          val selected = selectedSortOrder == order
          FilterChip(
            selected = selected,
            onClick = { onSelectSortOrder(order) },
            label = { Text(order.label, style = NavLabelStyle) },
            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("sub_sort_${order.name.lowercase()}"),
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = selected,
              borderColor = DarkBorder,
              selectedBorderColor = ElectricViolet
            ),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = ElectricViolet.copy(alpha = 0.25f),
              selectedLabelColor = ChampagneGold,
              containerColor = RoyalSurfaceHigh,
              labelColor = Lavender
            )
          )
        }
        FilterChip(
          selected = waitingFirst,
          onClick = onToggleWaitingFirst,
          label = { Text("Waiting first", style = NavLabelStyle) },
          modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("sub_sort_waiting_first"),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = waitingFirst,
            borderColor = DarkBorder,
            selectedBorderColor = ChampagneGold
          ),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ChampagneGold.copy(alpha = 0.2f),
            selectedLabelColor = ChampagneGold,
            containerColor = RoyalSurfaceHigh,
            labelColor = Lavender
          )
        )
      }

      if (isLoading && allSubmissions.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = MetallicGold)
        }
      } else if (displayedSubmissions.isEmpty()) {
        EmptyState(
          icon = Icons.Default.AssignmentTurnedIn,
          title = "No Papers Found",
          message = if (allSubmissions.isEmpty()) {
            "Student submissions will appear here once submitted."
          } else {
            "No papers match this filter or search."
          }
        )
      } else {
        displayedSubmissions.forEachIndexed { index, row ->
          val badgeColor = when (row.markingStatus) {
            MarkingStatus.RELEASED -> SuccessColor
            MarkingStatus.MARKING -> ElectricViolet
            MarkingStatus.PENDING -> ChampagneGold
            MarkingStatus.AUTO -> Lavender
          }
          RoyalCard(
            borderColor = if (row.markingStatus == MarkingStatus.PENDING) ChampagneGold.copy(alpha = 0.65f) else null,
            onClick = { onOpenStudentPaper(row) },
            modifier = Modifier.testTag("submission_row_${row.studentId}")
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .background(MetallicGold.copy(alpha = 0.16f))
                  .border(1.dp, ChampagneGold.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                  .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "#${index + 1}",
                  style = FieldLabelStyle.copy(fontSize = 12.sp, color = ChampagneGold)
                )
              }

              Spacer(modifier = Modifier.width(10.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(row.fullName, style = CardTitleStyle.copy(fontSize = 17.sp), modifier = Modifier.weight(1f, fill = false))
                  if (row.late) {
                    Text("LATE", style = FieldLabelStyle.copy(fontSize = 11.sp, color = ErrorColor))
                  }
                  if (row.focusLossCount > 0) {
                    Text("EXIT(${row.focusLossCount})", style = FieldLabelStyle.copy(fontSize = 11.sp, color = ErrorColor))
                  }
                }
                val rel = NotificationTimeFormatter.formatRelative(row.submittedAt)
                val subLine = listOf(row.admissionNumber, rel).filter { it.isNotBlank() && it != "-" }.joinToString(" • ")
                if (subLine.isNotBlank()) {
                  Text(subLine, style = CaptionStyle)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(badgeColor.copy(alpha = 0.16f))
                      .border(1.dp, badgeColor.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                      .padding(horizontal = 8.dp, vertical = 2.dp)
                  ) {
                    Text(row.markingStatus.label.uppercase(), style = FieldLabelStyle.copy(fontSize = 11.sp, color = badgeColor))
                  }
                  if (row.isReadyForBatchReturn) {
                    Text("READY", style = FieldLabelStyle.copy(fontSize = 10.sp, color = SuccessColor))
                  }
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              Column(horizontalAlignment = Alignment.End) {
                if (row.finalScore != null) {
                  Text(
                    text = "${formatMarks(row.finalScore)}/${formatMarks(row.maxTotal)}",
                    fontFamily = PlayfairDisplayFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ChampagneGold
                  )
                } else {
                  Icon(Icons.Default.Draw, contentDescription = "Mark paper", tint = ChampagneGold, modifier = Modifier.size(22.dp))
                  Text("Open", style = CaptionStyle.copy(color = ChampagneGold))
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }

    if (showBatchConfirm) {
      ConfirmActionDialog(
        title = "Return $readyToBatchReturnCount Marked Papers?",
        message = "This will generate & upload the official marked PDFs for all $readyToBatchReturnCount ready paper(s), publish their final scores, and notify those students.",
        confirmButtonText = "Return All Now",
        isLoading = isBatchReleasing,
        onConfirm = {
          showBatchConfirm = false
          batchSwitchChecked = false
          onReleaseAllReady()
        },
        onDismiss = {
          showBatchConfirm = false
          batchSwitchChecked = false
        }
      )
    }
  }
}
