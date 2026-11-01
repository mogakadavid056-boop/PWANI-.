package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "classrep_session")

class SessionManager(private val context: Context) {

  companion object {
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_EMAIL = stringPreferencesKey("email")
    private val KEY_FULL_NAME = stringPreferencesKey("full_name")
    private val KEY_ADMISSION_NUMBER = stringPreferencesKey("admission_number")
    private val KEY_ROLE = stringPreferencesKey("role")
    private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val KEY_LAST_CLASS_ID = stringPreferencesKey("last_class_id")
    private val KEY_IS_VERIFIED = androidx.datastore.preferences.core.booleanPreferencesKey("is_verified")
  }

  val sessionFlow: Flow<UserSession?> = context.dataStore.data
    .catch { _ ->
      emit(emptyPreferences())
    }
    .map { prefs ->
      val userId = prefs[KEY_USER_ID]
      val accessToken = prefs[KEY_ACCESS_TOKEN]
      if (!userId.isNullOrBlank() && !accessToken.isNullOrBlank()) {
        UserSession(
          userId = userId,
          email = prefs[KEY_EMAIL].orEmpty(),
          fullName = prefs[KEY_FULL_NAME].orEmpty(),
          admissionNumber = prefs[KEY_ADMISSION_NUMBER].orEmpty(),
          role = prefs[KEY_ROLE].orEmpty().ifBlank { "Student" },
          accessToken = accessToken,
          refreshToken = prefs[KEY_REFRESH_TOKEN],
          isVerified = prefs[KEY_IS_VERIFIED] ?: false
        )
      } else {
        null
      }
    }

  suspend fun getSession(): UserSession? = withContext(Dispatchers.IO) {
    try {
      withTimeoutOrNull(600L) {
        sessionFlow.firstOrNull()
      }
    } catch (_: Throwable) {
      null
    }
  }

  suspend fun saveSession(session: UserSession) = withContext(Dispatchers.IO) {
    try {
      context.dataStore.edit { prefs ->
        prefs[KEY_USER_ID] = session.userId
        prefs[KEY_EMAIL] = session.email
        prefs[KEY_FULL_NAME] = session.fullName
        prefs[KEY_ADMISSION_NUMBER] = session.admissionNumber
        prefs[KEY_ROLE] = session.role
        prefs[KEY_ACCESS_TOKEN] = session.accessToken
        prefs[KEY_IS_VERIFIED] = session.isVerified
        if (!session.refreshToken.isNullOrBlank()) {
          prefs[KEY_REFRESH_TOKEN] = session.refreshToken
        }
      }
    } catch (_: Throwable) {}
  }

  suspend fun updateRole(newRole: String) = withContext(Dispatchers.IO) {
    try {
      context.dataStore.edit { prefs ->
        prefs[KEY_ROLE] = newRole
      }
    } catch (_: Throwable) {}
  }

  suspend fun updateRoleAndVerification(newRole: String, isVerified: Boolean) = withContext(Dispatchers.IO) {
    try {
      context.dataStore.edit { prefs ->
        prefs[KEY_ROLE] = newRole
        prefs[KEY_IS_VERIFIED] = isVerified
      }
    } catch (_: Throwable) {}
  }

  suspend fun updateAdmissionNumber(admissionNumber: String) = withContext(Dispatchers.IO) {
    try {
      context.dataStore.edit { prefs ->
        prefs[KEY_ADMISSION_NUMBER] = admissionNumber
      }
    } catch (_: Throwable) {}
  }

  suspend fun saveLastClassId(classId: String) = withContext(Dispatchers.IO) {
    try {
      context.dataStore.edit { prefs ->
        prefs[KEY_LAST_CLASS_ID] = classId
      }
    } catch (_: Throwable) {}
  }

  suspend fun getLastClassId(): String? = withContext(Dispatchers.IO) {
    try {
      context.dataStore.data.map { prefs -> prefs[KEY_LAST_CLASS_ID] }.firstOrNull()
    } catch (_: Throwable) {
      null
    }
  }

  suspend fun clearSession() = withContext(Dispatchers.IO) {
    try {
      context.dataStore.edit { prefs ->
        prefs.clear()
      }
    } catch (_: Throwable) {}
  }
}
