package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdminUserItem
import com.example.ui.components.AdminTopBar
import com.example.ui.components.AdminUserRow
import com.example.ui.components.AssignRoleDialog
import com.example.ui.components.ClassRepPullToRefresh
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LoadingSkeleton
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ManageUsersScreen(
  users: List<AdminUserItem>,
  searchQuery: String,
  isLoading: Boolean,
  isActionLoading: Boolean,
  errorMessage: String?,
  successMessage: String?,
  selectedUser: AdminUserItem?,
  selectedRoleChoice: String,
  showConfirmDialog: Boolean,
  verificationPendingUser: AdminUserItem? = null,
  verificationTargetStatus: Boolean = false,
  showVerificationConfirmDialog: Boolean = false,
  onSearchQueryChanged: (String) -> Unit,
  onRefresh: () -> Unit,
  onUserClick: (AdminUserItem) -> Unit,
  onSelectRole: (String) -> Unit,
  onPromptConfirmRole: () -> Unit,
  onDismissConfirmRole: () -> Unit,
  onConfirmSaveRole: () -> Unit,
  onDismissRoleDialog: () -> Unit,
  onToggleVerification: (AdminUserItem) -> Unit = {},
  onDismissVerificationDialog: () -> Unit = {},
  onConfirmToggleVerification: () -> Unit = {},
  onDismissError: () -> Unit,
  onDismissSuccess: () -> Unit,
  onBack: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
      .testTag("manage_users_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
      AdminTopBar(onBack = onBack, onRefresh = onRefresh)

      Spacer(modifier = Modifier.height(14.dp))

      // Search Box (searches name, email and admission number)
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("admin_search_input"),
        placeholder = {
          Text("Search name, email, admission no...", color = TextSecondary, fontSize = 13.sp)
        },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChanged("") }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = DarkSurface,
          unfocusedContainerColor = DarkSurface,
          focusedBorderColor = GoldPrimary,
          unfocusedBorderColor = DarkBorder,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        )
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Error banner
      if (!errorMessage.isNullOrBlank()) {
        ErrorBanner(message = errorMessage, onDismiss = onDismissError)
        Spacer(modifier = Modifier.height(10.dp))
      }

      // Success banner
      if (!successMessage.isNullOrBlank()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SuccessContainer)
            .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = successMessage, color = SuccessGreen, fontSize = 13.sp)
            IconButton(onClick = onDismissSuccess, modifier = Modifier.size(24.dp)) {
              Icon(Icons.Default.Clear, contentDescription = "Dismiss", tint = SuccessGreen, modifier = Modifier.size(14.dp))
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      // User list header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (searchQuery.isBlank()) "All Users" else "Search Results",
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = GoldLight
        )
        Text(
          text = "${users.size} loaded",
          fontSize = 12.sp,
          color = TextSecondary
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // List or Loading
      ClassRepPullToRefresh(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
      ) {
        if (isLoading && users.isEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LoadingSkeleton(height = 65)
            LoadingSkeleton(height = 65)
            LoadingSkeleton(height = 65)
          }
        } else if (users.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(DarkCard)
              .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
              .padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.People, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(44.dp))
              Spacer(modifier = Modifier.height(10.dp))
              Text("No users found", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = GoldLight)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = if (searchQuery.isNotBlank()) "No users match '$searchQuery'" else "No user profiles available",
                fontSize = 12.sp,
                color = TextSecondary
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
          ) {
            items(users, key = { it.id }) { user ->
              AdminUserRow(
                user = user,
                onClick = { onUserClick(user) },
                onToggleVerification = { onToggleVerification(user) }
              )
            }
          }
        }
      }
    }

    // Role assignment dialog
    if (selectedUser != null) {
      AssignRoleDialog(
        user = selectedUser,
        selectedRoleChoice = selectedRoleChoice,
        showConfirmDialog = showConfirmDialog,
        isActionLoading = isActionLoading,
        onSelectRole = onSelectRole,
        onPromptConfirm = onPromptConfirmRole,
        onDismissConfirm = onDismissConfirmRole,
        onConfirmSave = onConfirmSaveRole,
        onDismiss = onDismissRoleDialog
      )
    }

    if (showVerificationConfirmDialog && verificationPendingUser != null) {
      androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissVerificationDialog,
        title = {
          Text(
            text = if (verificationTargetStatus) "Verify Lecturer" else "Remove Verification",
            color = GoldLight,
            fontWeight = FontWeight.Bold
          )
        },
        text = {
          Text(
            text = if (verificationTargetStatus) {
              "Are you sure you want to verify ${verificationPendingUser.fullName} as an official lecturer?"
            } else {
              "Are you sure you want to remove lecturer verification for ${verificationPendingUser.fullName}?"
            },
            color = TextSecondary
          )
        },
        confirmButton = {
          androidx.compose.material3.Button(
            onClick = onConfirmToggleVerification,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
              containerColor = if (verificationTargetStatus) GoldPrimary else com.example.ui.theme.ErrorRed,
              contentColor = com.example.ui.theme.DarkTextOnGold
            )
          ) {
            Text(if (verificationTargetStatus) "Verify" else "Remove")
          }
        },
        dismissButton = {
          androidx.compose.material3.TextButton(onClick = onDismissVerificationDialog) {
            Text("Cancel", color = TextSecondary)
          }
        },
        containerColor = DarkCard,
        shape = RoundedCornerShape(16.dp)
      )
    }
  }
}
