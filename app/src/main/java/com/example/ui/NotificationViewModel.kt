package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserSession
import com.example.notifications.AppNotificationDispatcher
import com.example.notifications.AppNotificationItem
import com.example.notifications.NotificationDeepLink
import com.example.notifications.NotificationPreferences
import com.example.notifications.NotificationService
import com.example.notifications.NotificationSyncWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationUiState(
  val unreadCount: Int = 0,
  val notifications: List<AppNotificationItem> = emptyList(),
  val isLoading: Boolean = false,
  val isLoadingMore: Boolean = false,
  val hasMore: Boolean = false,
  val isMarkingAll: Boolean = false,
  val errorMessage: String? = null,
  val pendingDeepLink: NotificationDeepLink? = null
)

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
  private val service = NotificationService()
  private val _uiState = MutableStateFlow(NotificationUiState())
  val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

  private var pollingJob: Job? = null
  private val pageSize = 30

  fun startSessionSync(session: UserSession?) {
    if (session == null || session.accessToken.isBlank()) {
      stopAndClear()
      return
    }
    NotificationSyncWorker.schedule(getApplication())
    pollingJob?.cancel()
    pollingJob = viewModelScope.launch {
      while (true) {
        pollOnce(session)
        delay(20_000L)
      }
    }
  }

  private suspend fun pollOnce(session: UserSession) {
    service.fetchUnreadCount(session.accessToken).onSuccess { count ->
      val prev = _uiState.value.unreadCount
      _uiState.update { it.copy(unreadCount = count) }
      if (count > 0 && (count != prev || _uiState.value.notifications.isEmpty())) {
        service.fetchNotifications(session.accessToken, limit = 20, offset = 0).onSuccess { list ->
          AppNotificationDispatcher.dispatchUnseen(getApplication(), list)
        }
      }
    }
  }

  fun refresh(session: UserSession?) {
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      service.fetchNotifications(session.accessToken, limit = pageSize, offset = 0).fold(
        onSuccess = { list ->
          val unread = list.count { it.isUnread }
          _uiState.update {
            it.copy(
              notifications = list,
              unreadCount = maxOf(unread, it.unreadCount),
              isLoading = false,
              hasMore = list.size >= pageSize
            )
          }
          service.fetchUnreadCount(session.accessToken).onSuccess { c ->
            _uiState.update { it.copy(unreadCount = c) }
          }
        },
        onFailure = { err ->
          _uiState.update { it.copy(isLoading = false, errorMessage = err.message ?: "Failed to load notifications") }
        }
      )
    }
  }

  fun loadMore(session: UserSession?) {
    if (session == null || session.accessToken.isBlank()) return
    val current = _uiState.value
    if (current.isLoadingMore || !current.hasMore) return
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
      service.fetchNotifications(session.accessToken, limit = pageSize, offset = current.notifications.size).fold(
        onSuccess = { more ->
          val merged = (current.notifications + more).distinctBy { it.id }
          _uiState.update { it.copy(notifications = merged, isLoadingMore = false, hasMore = more.size >= pageSize) }
        },
        onFailure = { err ->
          _uiState.update { it.copy(isLoadingMore = false, errorMessage = err.message) }
        }
      )
    }
  }

  fun onNotificationClick(item: AppNotificationItem, session: UserSession?) {
    if (item.isUnread && session != null && session.accessToken.isNotBlank()) {
      _uiState.update { st ->
        st.copy(
          notifications = st.notifications.map { if (it.id == item.id) it.copy(readAt = "read") else it },
          unreadCount = (st.unreadCount - 1).coerceAtLeast(0)
        )
      }
      viewModelScope.launch {
        service.markRead(listOf(item.id), session.accessToken)
      }
    }
    _uiState.update {
      it.copy(pendingDeepLink = NotificationDeepLink(item.kind, item.targetClassId, item.data))
    }
  }

  fun markAllAsRead(session: UserSession?) {
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isMarkingAll = true, errorMessage = null) }
      service.markRead(null, session.accessToken).fold(
        onSuccess = {
          _uiState.update { st ->
            st.copy(
              isMarkingAll = false,
              unreadCount = 0,
              notifications = st.notifications.map { if (it.isUnread) it.copy(readAt = "read") else it }
            )
          }
        },
        onFailure = { err ->
          _uiState.update { it.copy(isMarkingAll = false, errorMessage = err.message ?: "Could not mark notifications read") }
        }
      )
    }
  }

  fun triggerDeepLink(link: NotificationDeepLink, session: UserSession?) {
    val notifId = link.data["notification_id"]
    if (!notifId.isNullOrBlank() && session != null && session.accessToken.isNotBlank()) {
      viewModelScope.launch { service.markRead(listOf(notifId), session.accessToken) }
    }
    _uiState.update { it.copy(pendingDeepLink = link) }
  }

  fun consumeDeepLink() { _uiState.update { it.copy(pendingDeepLink = null) } }
  fun clearError() { _uiState.update { it.copy(errorMessage = null) } }

  fun stopAndClear() {
    pollingJob?.cancel()
    pollingJob = null
    NotificationSyncWorker.cancel(getApplication())
    NotificationPreferences.clearOnLogout(getApplication())
    _uiState.value = NotificationUiState()
  }
}
