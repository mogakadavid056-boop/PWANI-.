package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClassItem
import com.example.data.ClassWaitingForPrefect
import com.example.data.NoticeItem
import com.example.data.UserSession
import com.example.data.formatRoleDisplayName
import com.example.ui.components.ClassRepCard
import com.example.ui.components.ClassRepPullToRefresh
import com.example.ui.components.ClassesWaitingForPrefectCard
import com.example.ui.components.FriendlyErrorSnackbar
import com.example.ui.components.LatestAnnouncementsSection
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.*

@Composable
fun HomeScreen(
  session: UserSession?,
  latestAnnouncements: List<NoticeItem> = emptyList(),
  isLatestLoading: Boolean = false,
  classes: List<ClassItem> = emptyList(),
  isClassesLoading: Boolean = false,
  classesError: String? = null,
  classesWaitingForRep: List<ClassWaitingForPrefect> = emptyList(),
  isWaitingLoading: Boolean = false,
  onAppointPrefectClick: (ClassWaitingForPrefect) -> Unit = {},
  onDismissClassesError: () -> Unit = {},
  onRefreshClasses: () -> Unit = {},
  onNavigateToJoinClass: () -> Unit = {},
  onNavigateToCreateClass: () -> Unit = {},
  onNavigateToFindClasses: () -> Unit = {},
  onNavigateToMyClasses: () -> Unit = {},
  onNavigateToManageUsers: () -> Unit = {},
  onNavigateToProfile: () -> Unit = {},
  onSelectClass: (ClassItem) -> Unit = {},
  onNoticeClick: (NoticeItem) -> Unit = {},
  onLogout: () -> Unit = {}
) {
  val rawRole = session?.role.orEmpty().ifBlank { "student" }
  val isAdmin = rawRole.equals("admin", ignoreCase = true)
  val isLecturer = rawRole.equals("lecturer", ignoreCase = true)
  val isStaff = isAdmin || isLecturer
  val isVerified = session?.isVerified == true
  val displayRole = formatRoleDisplayName(rawRole)

  val firstName = session?.fullName?.trim()
    ?.split("\\s+".toRegex())
    ?.firstOrNull()
    ?.ifBlank { "Member" } ?: "Member"
  val greeting = "Good morning, $firstName"

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
      .testTag("home_screen")
  ) {
    ClassRepPullToRefresh(
      isRefreshing = isLatestLoading || isClassesLoading,
      onRefresh = onRefreshClasses
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = ScreenPadding, vertical = ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(ItemSpacing)
      ) {
      // Header: Greeting and Role Chip
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = greeting,
            style = TitleStyle,
            modifier = Modifier.testTag("home_greeting_text")
          )
          Spacer(modifier = Modifier.height(4.dp))
          // Role chip
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(GoldPrimary.copy(alpha = 0.15f))
              .border(1.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp, vertical = 3.dp)
              .testTag("home_role_chip")
          ) {
            Text(
              text = displayRole,
              style = CaptionStyle.copy(
                color = GoldAccent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
              )
            )
          }
        }

        IconButton(
          onClick = onNavigateToProfile,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, CircleShape)
            .testTag("home_profile_avatar_button")
        ) {
          Icon(
            Icons.Default.Person,
            contentDescription = "Profile",
            tint = GoldLight,
            modifier = Modifier.size(24.dp)
          )
        }
      }

      if (!classesError.isNullOrBlank()) {
        FriendlyErrorSnackbar(errorMessage = classesError, onDismiss = onDismissClassesError)
      }

      // Lecturer verification banner if unverified
      if (isLecturer && !isVerified) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(androidx.compose.ui.graphics.Color(0xFF332005))
            .border(1.dp, androidx.compose.ui.graphics.Color(0xFFFFB300), RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("home_lecturer_unverified_banner")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.Info,
              contentDescription = null,
              tint = androidx.compose.ui.graphics.Color(0xFFFFB300),
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Your lecturer account is waiting for verification by an administrator.",
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              color = androidx.compose.ui.graphics.Color(0xFFFFE082),
              lineHeight = 18.sp
            )
          }
        }
      }

      // Quick Action Buttons depending on user role and verification
      if (isLecturer) {
        if (!isVerified) {
          PrimaryButton(
            text = "Join with class code",
            leadingIcon = Icons.Default.GroupAdd,
            onClick = onNavigateToJoinClass,
            modifier = Modifier.fillMaxWidth().testTag("home_join_class_button")
          )
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            PrimaryButton(
              text = "Find my classes",
              leadingIcon = Icons.Default.Search,
              onClick = onNavigateToFindClasses,
              modifier = Modifier.weight(1f).testTag("home_find_my_classes_button")
            )
            com.example.ui.components.ClassRepOutlinedButton(
              text = "My classes",
              leadingIcon = Icons.Default.Class,
              onClick = onNavigateToMyClasses,
              modifier = Modifier.weight(1f).testTag("home_my_classes_button")
            )
          }
        }
      } else if (isAdmin) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          PrimaryButton(
            text = "Find classes",
            leadingIcon = Icons.Default.Search,
            onClick = onNavigateToFindClasses,
            modifier = Modifier.weight(1f).testTag("home_admin_find_classes_button")
          )
          com.example.ui.components.ClassRepOutlinedButton(
            text = "Create Class",
            leadingIcon = Icons.Default.Add,
            onClick = onNavigateToCreateClass,
            modifier = Modifier.weight(1f).testTag("home_create_class_button")
          )
        }
      } else {
        // Student
        val studentHasClass = classes.isNotEmpty()
        if (!studentHasClass) {
          PrimaryButton(
            text = "Join Class",
            leadingIcon = Icons.Default.GroupAdd,
            onClick = onNavigateToJoinClass,
            modifier = Modifier.fillMaxWidth().testTag("home_join_class_button")
          )
        }
      }

      // Manage Users button ONLY when the role is admin
      if (isAdmin) {
        com.example.ui.components.ClassRepOutlinedButton(
          text = "Manage Users",
          leadingIcon = Icons.Default.SupervisorAccount,
          onClick = onNavigateToManageUsers,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("home_manage_users_button")
        )
      }

      // Classes waiting for a prefect (Admin & Lecturer)
      if (isStaff) {
        ClassesWaitingForPrefectCard(
          classes = classesWaitingForRep,
          isLoading = isWaitingLoading,
          onAppointClick = onAppointPrefectClick
        )
      }

      // Latest Announcements Section
      LatestAnnouncementsSection(
        announcements = latestAnnouncements,
        isLoading = isLatestLoading,
        onNoticeClick = onNoticeClick
      )

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
}
