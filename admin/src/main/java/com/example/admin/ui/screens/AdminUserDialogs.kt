package com.example.admin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.admin.data.AdminUserItem
import com.example.admin.ui.theme.AdminCardElevated
import com.example.admin.ui.theme.CrimsonError
import com.example.admin.ui.theme.DarkTextOnGold
import com.example.admin.ui.theme.GoldLight
import com.example.admin.ui.theme.GoldPrimary
import com.example.admin.ui.theme.TextPrimary
import com.example.admin.ui.theme.TextSecondary
import com.example.admin.util.AdminRoleHelper

@Composable
fun AdminAssignRoleDialog(
  user: AdminUserItem,
  isActionLoading: Boolean,
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var chosenRole by remember(user.id) {
    mutableStateOf(AdminRoleHelper.normalizeRole(user.role).ifBlank { AdminRoleHelper.ROLE_STUDENT })
  }
  val options = listOf(
    AdminRoleHelper.ROLE_STUDENT to "Student",
    AdminRoleHelper.ROLE_CLASS_REP to "Class Rep",
    AdminRoleHelper.ROLE_LECTURER to "Lecturer",
    AdminRoleHelper.ROLE_ADMIN to "Super Admin"
  )

  AlertDialog(
    onDismissRequest = { if (!isActionLoading) onDismiss() },
    containerColor = AdminCardElevated,
    shape = RoundedCornerShape(16.dp),
    title = {
      Column {
        Text("Assign Role", fontWeight = FontWeight.Bold, color = GoldLight, fontSize = 18.sp)
        Text(user.fullName.ifBlank { user.email }, fontSize = 13.sp, color = TextSecondary)
      }
    },
    text = {
      Column {
        options.forEach { (roleKey, label) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable(enabled = !isActionLoading) { chosenRole = roleKey }
              .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = chosenRole == roleKey,
              onClick = { chosenRole = roleKey },
              colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary, unselectedColor = TextSecondary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = TextPrimary, fontSize = 15.sp)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(chosenRole) },
        enabled = !isActionLoading,
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DarkTextOnGold)
      ) { Text("Save Role") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isActionLoading) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}

@Composable
fun AdminVerifyDialog(
  user: AdminUserItem,
  isActionLoading: Boolean,
  onConfirm: (Boolean) -> Unit,
  onDismiss: () -> Unit
) {
  val targetVerified = !user.isVerified
  AlertDialog(
    onDismissRequest = { if (!isActionLoading) onDismiss() },
    containerColor = AdminCardElevated,
    shape = RoundedCornerShape(16.dp),
    title = {
      Text(
        text = if (targetVerified) "Restore Lecturer Access" else "Suspend Lecturer Access",
        fontWeight = FontWeight.Bold,
        color = GoldLight
      )
    },
    text = {
      Text(
        text = if (targetVerified) {
          "Restore lecturer access for ${user.fullName.ifBlank { user.email }} so they can manage classes and exams?"
        } else {
          "Suspend lecturer access for ${user.fullName.ifBlank { user.email }}? They will remain blocked until restored."
        },
        color = TextSecondary
      )
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(targetVerified) },
        enabled = !isActionLoading,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (targetVerified) GoldPrimary else CrimsonError,
          contentColor = DarkTextOnGold
        )
      ) { Text(if (targetVerified) "Restore" else "Suspend") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isActionLoading) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}
