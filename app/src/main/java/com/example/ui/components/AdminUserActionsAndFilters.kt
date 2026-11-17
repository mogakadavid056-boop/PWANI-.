package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminUserItem
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkTextOnGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AdminFilterChipsRow(
  selectedTab: String,
  pendingCount: Int,
  onSelectTab: (String) -> Unit
) {
  val tabs = listOf(
    "all" to "All",
    "pending" to if (pendingCount > 0) "Pending ($pendingCount)" else "Pending",
    "class_rep" to "Class Reps",
    "lecturer" to "Lecturers",
    "student" to "Students"
  )
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    tabs.forEach { (key, label) ->
      val selected = selectedTab == key
      FilterChip(
        selected = selected,
        onClick = { onSelectTab(key) },
        label = {
          Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
          )
        },
        modifier = Modifier.testTag("admin_filter_$key"),
        colors = FilterChipDefaults.filterChipColors(
          containerColor = DarkSurface,
          labelColor = TextSecondary,
          selectedContainerColor = GoldPrimary,
          selectedLabelColor = DarkTextOnGold
        ),
        border = FilterChipDefaults.filterChipBorder(
          enabled = true,
          selected = selected,
          borderColor = DarkBorder,
          selectedBorderColor = GoldPrimary
        )
      )
    }
  }
}

@Composable
fun AdminApprovalConfirmDialog(
  user: AdminUserItem,
  approve: Boolean,
  isActionLoading: Boolean,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  val actionTitle = if (approve) "Approve Lecturer" else "Reject Lecturer"
  val message = if (approve) {
    "Approve ${user.fullName.ifBlank { "this lecturer" }} (${user.staffNumber.ifBlank { user.email }}) for lecturer access?"
  } else {
    "Reject the lecturer account application for ${user.fullName.ifBlank { "this user" }}?"
  }
  AlertDialog(
    onDismissRequest = { if (!isActionLoading) onDismiss() },
    title = { Text(actionTitle, color = GoldLight, fontWeight = FontWeight.Bold) },
    text = { Text(message, color = TextSecondary) },
    confirmButton = {
      Button(
        onClick = onConfirm,
        enabled = !isActionLoading,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (approve) GoldPrimary else ErrorRed,
          contentColor = DarkTextOnGold
        ),
        modifier = Modifier.testTag("admin_confirm_approval_btn")
      ) {
        if (isActionLoading) {
          CircularProgressIndicator(color = DarkTextOnGold, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        } else {
          Text(if (approve) "Approve" else "Reject", fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isActionLoading) {
        Text("Cancel", color = TextSecondary)
      }
    },
    containerColor = DarkCard,
    shape = RoundedCornerShape(16.dp)
  )
}

@Composable
fun AdminSetRepConfirmDialog(
  user: AdminUserItem,
  makeRep: Boolean,
  isActionLoading: Boolean,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = { if (!isActionLoading) onDismiss() },
    title = {
      Text(
        text = if (makeRep) "Appoint Class Rep" else "Remove Class Rep",
        color = GoldLight,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Text(
        text = if (makeRep) {
          "Appoint ${user.fullName.ifBlank { "this student" }} as a Class Rep?"
        } else {
          "Remove the Class Rep role from ${user.fullName.ifBlank { "this student" }}?"
        },
        color = TextSecondary
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        enabled = !isActionLoading,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (makeRep) GoldPrimary else ErrorRed,
          contentColor = DarkTextOnGold
        ),
        modifier = Modifier.testTag("admin_confirm_rep_btn")
      ) {
        if (isActionLoading) {
          CircularProgressIndicator(color = DarkTextOnGold, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        } else {
          Text(if (makeRep) "Appoint" else "Remove", fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isActionLoading) {
        Text("Cancel", color = TextSecondary)
      }
    },
    containerColor = DarkCard,
    shape = RoundedCornerShape(16.dp)
  )
}

@Composable
fun AdminVerifyLecturerConfirmDialog(
  user: AdminUserItem,
  targetVerified: Boolean,
  isActionLoading: Boolean,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = { if (!isActionLoading) onDismiss() },
    title = {
      Text(
        text = if (targetVerified) "Verify Lecturer" else "Remove Verification",
        color = GoldLight,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Text(
        text = if (targetVerified) {
          "Are you sure you want to verify ${user.fullName} as an official lecturer?"
        } else {
          "Are you sure you want to remove lecturer verification for ${user.fullName}?"
        },
        color = TextSecondary
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        enabled = !isActionLoading,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (targetVerified) GoldPrimary else ErrorRed,
          contentColor = DarkTextOnGold
        )
      ) {
        Text(if (targetVerified) "Verify" else "Remove")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isActionLoading) {
        Text("Cancel", color = TextSecondary)
      }
    },
    containerColor = DarkCard,
    shape = RoundedCornerShape(16.dp)
  )
}
