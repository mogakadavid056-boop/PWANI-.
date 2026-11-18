package com.example.ui

import com.example.data.UserSession

sealed interface Screen {
  data object Welcome : Screen
  data object Landing : Screen
  data object Register : Screen
  data object Login : Screen
  data object Home : Screen
  data object JoinClass : Screen
  data object CreateClass : Screen
  data object ClassDetail : Screen
  data object PostNotice : Screen
  data object ManageMembers : Screen
  data object GroupChat : Screen
  data object GroupInfo : Screen
  data object Profile : Screen
  data object ManageUsers : Screen
  data object Waiting : Screen
  data object FindClasses : Screen
}

data class AuthUiState(
  val currentScreen: Screen = Screen.Welcome,
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val infoMessage: String? = null,
  val session: UserSession? = null
)
