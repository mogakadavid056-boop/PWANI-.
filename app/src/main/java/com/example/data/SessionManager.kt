package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    private val KEY_IS_VERIFIED = booleanPreferencesKey("is_verified")
    private val KEY_PENDING_REG_JSON = stringPreferencesKey("pending_reg_json")
  }

  data class PendingRegistrationData(
    val email: String,
    val fullName: String = "",
    val isLecturer: Boolean,
    val idNumber: String,
    val phone: String,
    val universityId: String,
    val schoolId: String,
    val programmeId: String,
    val yearOfStudy: Int?,
    val knowsDetails: Boolean,
    val classCode: String,
    val acceptedTermsAt: String,
    val termsVersion: String,
    val lecturerPasscode: String = ""
  )

  val sessionFlow: Flow<UserSession?> = context.dataStore.data
    .catch { _ -> emit(emptyPreferences()) }
    .map { prefs ->
      val userId = prefs[KEY_USER_ID]
      val accessToken = prefs[KEY_ACCESS_TOKEN]
      if (!userId.isNullOrBlank() && !accessToken.isNullOrBlank()) {
        UserSession(
          userId = userId,
          email = prefs[KEY_EMAIL].orEmpty(),
          fullName = prefs[KEY_FULL_NAME].orEmpty(),
          admissionNumber = prefs[KEY_ADMISSION_NUMBER].orEmpty(),
          role = prefs[KEY_ROLE].orEmpty().ifBlank { "student" },
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
      withTimeoutOrNull(600L) { sessionFlow.firstOrNull() }
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

  suspend fun savePendingRegistration(data: PendingRegistrationData) = withContext(Dispatchers.IO) {
    try {
      val json = org.json.JSONObject().apply {
        put("email", data.email.trim().lowercase())
        if (data.fullName.isNotBlank()) put("full_name", data.fullName.trim())
        put("is_lecturer", data.isLecturer)
        put("id_number", data.idNumber)
        put("phone", data.phone)
        put("university_id", data.universityId)
        put("school_id", data.schoolId)
        put("programme_id", data.programmeId)
        if (data.yearOfStudy != null) put("year_of_study", data.yearOfStudy)
        put("knows_details", data.knowsDetails)
        put("class_code", data.classCode)
        put("accepted_terms_at", data.acceptedTermsAt)
        put("terms_version", data.termsVersion)
        if (data.lecturerPasscode.isNotBlank()) put("lecturer_passcode", data.lecturerPasscode)
      }
      context.dataStore.edit { prefs -> prefs[KEY_PENDING_REG_JSON] = json.toString() }
    } catch (_: Throwable) {}
  }

  suspend fun getPendingRegistration(email: String): PendingRegistrationData? = withContext(Dispatchers.IO) {
    try {
      val raw = context.dataStore.data.map { it[KEY_PENDING_REG_JSON] }.firstOrNull() ?: return@withContext null
      val o = org.json.JSONObject(raw)
      if (!o.optString("email").equals(email.trim(), ignoreCase = true)) return@withContext null
      PendingRegistrationData(
        email = o.optString("email"),
        fullName = o.optString("full_name"),
        isLecturer = o.optBoolean("is_lecturer", false),
        idNumber = o.optString("id_number"),
        phone = o.optString("phone"),
        universityId = o.optString("university_id"),
        schoolId = o.optString("school_id"),
        programmeId = o.optString("programme_id"),
        yearOfStudy = if (o.has("year_of_study") && !o.isNull("year_of_study")) o.optInt("year_of_study") else null,
        knowsDetails = o.optBoolean("knows_details", true),
        classCode = o.optString("class_code"),
        acceptedTermsAt = o.optString("accepted_terms_at"),
        termsVersion = o.optString("terms_version", "1.0"),
        lecturerPasscode = o.optString("lecturer_passcode", "")
      )
    } catch (_: Throwable) { null }
  }

  suspend fun clearPendingRegistration() = withContext(Dispatchers.IO) {
    try {
      context.dataStore.edit { prefs -> prefs.remove(KEY_PENDING_REG_JSON) }
    } catch (_: Throwable) {}
  }

  suspend fun updateRole(newRole: String) = withContext(Dispatchers.IO) {
    try {
      context.dataStore.edit { prefs -> prefs[KEY_ROLE] = newRole }
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
    try { context.dataStore.edit { prefs -> prefs[KEY_ADMISSION_NUMBER] = admissionNumber } } catch (_: Throwable) {}
  }

  suspend fun updateMemberDetails(fullName: String, admissionNumber: String) = withContext(Dispatchers.IO) {
    try {
      context.dataStore.edit { prefs ->
        prefs[KEY_FULL_NAME] = fullName.trim()
        prefs[KEY_ADMISSION_NUMBER] = admissionNumber.trim()
      }
    } catch (_: Throwable) {}
  }

  suspend fun saveLastClassId(classId: String) = withContext(Dispatchers.IO) {
    try { context.dataStore.edit { prefs -> prefs[KEY_LAST_CLASS_ID] = classId } } catch (_: Throwable) {}
  }

  suspend fun getLastClassId(): String? = withContext(Dispatchers.IO) {
    try { context.dataStore.data.map { prefs -> prefs[KEY_LAST_CLASS_ID] }.firstOrNull() } catch (_: Throwable) { null }
  }

  suspend fun clearSession() = withContext(Dispatchers.IO) {
    try { context.dataStore.edit { prefs -> prefs.clear() } } catch (_: Throwable) {}
  }
}
