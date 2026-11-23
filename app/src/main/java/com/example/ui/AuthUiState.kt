package com.example.ui

import com.example.data.UserSession

sealed interface Screen {
  data object Splash : Screen
  data object Welcome : Screen
  data object Landing : Screen
  data object Register : Screen
  data object CheckEmail : Screen
  data object RegisterResult : Screen
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
  data object Notifications : Screen
  data object Assistant : Screen
}

sealed interface RegisterResultState {
  data class StudentPlaced(
    val classId: String,
    val className: String,
    val hasPrefect: Boolean
  ) : RegisterResultState

  data class StudentNotPlaced(
    val message: String? = null
  ) : RegisterResultState

  data class LecturerPasscodeRight(
    val isVerified: Boolean
  ) : RegisterResultState

  data object LecturerPasscodeWrong : RegisterResultState
}

data class AuthUiState(
  val currentScreen: Screen = Screen.Splash,
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val infoMessage: String? = null,
  val session: UserSession? = null,
  val pendingConfirmationEmail: String? = null,
  val registerResult: RegisterResultState? = null,
  val pendingOpenClassId: String? = null
)
