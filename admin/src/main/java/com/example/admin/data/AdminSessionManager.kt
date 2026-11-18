package com.example.admin.data

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

private val Context.adminDataStore: DataStore<Preferences> by preferencesDataStore(name = "classrep_admin_session")

class AdminSessionManager(private val context: Context) {
  companion object {
    private val KEY_USER_ID = stringPreferencesKey("admin_user_id")
    private val KEY_EMAIL = stringPreferencesKey("admin_email")
    private val KEY_FULL_NAME = stringPreferencesKey("admin_full_name")
    private val KEY_ROLE = stringPreferencesKey("admin_role")
    private val KEY_ACCESS_TOKEN = stringPreferencesKey("admin_access_token")
    private val KEY_REFRESH_TOKEN = stringPreferencesKey("admin_refresh_token")
  }

  val sessionFlow: Flow<AdminSession?> = context.adminDataStore.data
    .catch { emit(emptyPreferences()) }
    .map { prefs ->
      val uid = prefs[KEY_USER_ID]
      val token = prefs[KEY_ACCESS_TOKEN]
      if (!uid.isNullOrBlank() && !token.isNullOrBlank()) {
        AdminSession(
          userId = uid,
          email = prefs[KEY_EMAIL].orEmpty(),
          fullName = prefs[KEY_FULL_NAME].orEmpty(),
          role = prefs[KEY_ROLE].orEmpty(),
          accessToken = token,
          refreshToken = prefs[KEY_REFRESH_TOKEN]
        )
      } else {
        null
      }
    }

  suspend fun getSession(): AdminSession? = withContext(Dispatchers.IO) {
    try {
      withTimeoutOrNull(800L) { sessionFlow.firstOrNull() }
    } catch (_: Throwable) {
      null
    }
  }

  suspend fun saveSession(session: AdminSession) = withContext(Dispatchers.IO) {
    try {
      context.adminDataStore.edit { prefs ->
        prefs[KEY_USER_ID] = session.userId
        prefs[KEY_EMAIL] = session.email
        prefs[KEY_FULL_NAME] = session.fullName
        prefs[KEY_ROLE] = session.role
        prefs[KEY_ACCESS_TOKEN] = session.accessToken
        if (!session.refreshToken.isNullOrBlank()) {
          prefs[KEY_REFRESH_TOKEN] = session.refreshToken
        }
      }
    } catch (_: Throwable) {}
  }

  suspend fun clearSession() = withContext(Dispatchers.IO) {
    try {
      context.adminDataStore.edit { it.clear() }
    } catch (_: Throwable) {}
  }
}
