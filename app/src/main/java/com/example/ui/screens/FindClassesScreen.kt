package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ClassItem
import com.example.data.SchoolItem
import com.example.data.SearchClassItem
import com.example.data.UserSession
import com.example.ui.ClassSearchUiState
import com.example.ui.ClassSearchViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun FindClassesScreen(
  session: UserSession?,
  isAdmin: Boolean,
  onBack: (() -> Unit)? = null,
  onOpenClass: (ClassItem) -> Unit,
  viewModel: ClassSearchViewModel = viewModel()
) {
  LaunchedEffect(session?.accessToken) {
    viewModel.setSession(session)
  }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  FindClassesContent(
    uiState = uiState,
    isAdmin = isAdmin,
    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
    onSchoolFilterChanged = { viewModel.onSchoolFilterChanged(it) },
    onYearFilterChanged = { viewModel.onYearFilterChanged(it) },
    onWithoutPrefectToggled = { viewModel.onWithoutPrefectToggled(it) },
    onRefresh = { viewModel.refresh() },
    onLoadMore = { viewModel.loadMore() },
    onOpenClass = onOpenClass,
    onAddClass = { item -> viewModel.claimClass(item, onSuccess = onOpenClass) },
    onDismissError = { viewModel.clearError() },
    onBack = onBack
  )
}

@Composable
fun FindClassesContent(
  uiState: ClassSearchUiState,
  isAdmin: Boolean,
  onSearchQueryChanged: (String) -> Unit,
  onSchoolFilterChanged: (SchoolItem?) -> Unit,
  onYearFilterChanged: (Int?) -> Unit,
  onWithoutPrefectToggled: (Boolean) -> Unit,
  onRefresh: () -> Unit,
  onLoadMore: () -> Unit,
  onOpenClass: (ClassItem) -> Unit,
  onAddClass: (SearchClassItem) -> Unit,
  onDismissError: () -> Unit,
  onBack: (() -> Unit)? = null
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
      .testTag("find_classes_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp).testTag("find_classes_back_btn")) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldLight)
            }
            Spacer(modifier = Modifier.width(6.dp))
          }
          Text(
            text = if (isAdmin && onBack == null) "Classes" else "Find My Classes",
            style = TitleStyle,
            modifier = Modifier.testTag("find_classes_title")
          )
        }

        IconButton(onClick = onRefresh, modifier = Modifier.size(40.dp).testTag("find_classes_refresh_btn")) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoldLight, modifier = Modifier.size(22.dp))
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      FindClassesSearchBar(
        query = uiState.searchQuery,
        onQueryChange = onSearchQueryChanged
      )

      Spacer(modifier = Modifier.height(10.dp))

      FindClassesSchoolDropdown(
        selectedSchool = uiState.selectedSchool,
        schools = uiState.schools,
        onSchoolSelected = onSchoolFilterChanged
      )

      Spacer(modifier = Modifier.height(10.dp))

      FindClassesYearChips(
        selectedYear = uiState.selectedYear,
        onYearSelected = onYearFilterChanged
      )

      if (isAdmin) {
        Spacer(modifier = Modifier.height(10.dp))
        FindClassesWithoutPrefectSwitch(
          onlyWithoutPrefect = uiState.onlyWithoutPrefect,
          onToggle = onWithoutPrefectToggled
        )
      }

      if (!uiState.errorMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        FriendlyErrorSnackbar(errorMessage = uiState.errorMessage, onDismiss = onDismissError)
      }

      Spacer(modifier = Modifier.height(10.dp))

      ClassRepPullToRefresh(
        isRefreshing = uiState.isLoading && uiState.results.isEmpty(),
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
      ) {
        if (uiState.isLoading && uiState.results.isEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LoadingSkeleton(height = 90)
            LoadingSkeleton(height = 90)
            LoadingSkeleton(height = 90)
          }
        } else if (uiState.results.isEmpty()) {
          EmptyState(
            icon = Icons.Default.Class,
            title = "No Classes Found",
            message = "No classes match your search and filter criteria."
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
          ) {
            items(uiState.results, key = { it.id }) { item ->
              SearchClassCard(
                item = item,
                isClaiming = uiState.isClaimingId == item.id,
                onOpen = {
                  onOpenClass(
                    ClassItem(id = item.id, name = item.className, code = item.classCode, programmeName = item.programmeName)
                  )
                },
                onAdd = { onAddClass(item) }
              )
            }

            if (uiState.canLoadMore) {
              item {
                Box(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                  contentAlignment = Alignment.Center
                ) {
                  if (uiState.isLoading) {
                    CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                  } else {
                    OutlinedButton(
                      onClick = onLoadMore,
                      shape = RoundedCornerShape(10.dp),
                      border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                      modifier = Modifier.testTag("find_classes_load_more_btn")
                    ) {
                      Text("Load more", color = GoldPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
