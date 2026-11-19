package com.example.admin.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.admin.data.AdminSession
import com.example.admin.notifications.AdminNotificationDeepLink
import com.example.admin.notifications.AdminNotificationDispatcher
import com.example.admin.notifications.AdminNotificationItem
import com.example.admin.notifications.AdminNotificationPrefs
import com.example.admin.notifications.AdminNotificationService
import com.example.admin.notifications.AdminNotificationWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AdminNotificationUiState(
  val items: List<AdminNotificationItem> = emptyList(),
  val unreadCount: Int = 0,
  val isLoading: Boolean = false,
  val isLoadingMore: Boolean = false,
  val hasMore: Boolean = false,
  val errorMessage: String? = null,
  val pendingDeepLink: AdminNotificationDeepLink? = null
)

class AdminNotificationViewModel(application: Application) : AndroidViewModel(application) {
  private val service = AdminNotificationService()
  private val _state = MutableStateFlow(AdminNotificationUiState())
  val state: StateFlow<AdminNotificationUiState> = _state.asStateFlow()

  private var pollJob: Job? = null
  private val pageSize = 30

  fun startSessionSync(session: AdminSession?) {
    if (session == null || session.accessToken.isBlank()) {
      stopAndClear()
      return
    }
    AdminNotificationWorker.schedule(getApplication())
    pollJob?.cancel()
    pollJob = viewModelScope.launch {
      refresh(session)
      while (isActive) {
        delay(20_000L)
        pollSilently(session)
      }
    }
  }

  fun refresh(session: AdminSession?) {
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      _state.update { it.copy(isLoading = true, errorMessage = null) }
      val countRes = service.fetchUnreadCount(session.accessToken)
      val listRes = service.fetchNotifications(session.accessToken, limit = pageSize, offset = 0)
      listRes.getOrNull()?.let { list ->
        AdminNotificationDispatcher.dispatchUnseen(getApplication(), list)
      }
      _state.update { s ->
        val list = listRes.getOrElse { s.items }
        s.copy(
          items = list,
          unreadCount = countRes.getOrElse { list.count { it.isUnread } },
          isLoading = false,
          hasMore = (listRes.getOrNull()?.size ?: 0) >= pageSize,
          errorMessage = listRes.exceptionOrNull()?.message
        )
      }
    }
  }

  private suspend fun pollSilently(session: AdminSession) {
    val countRes = service.fetchUnreadCount(session.accessToken)
    val unread = countRes.getOrNull() ?: return
    val prevUnread = _state.value.unreadCount
    _state.update { it.copy(unreadCount = unread) }
    if (unread > prevUnread || _state.value.items.isEmpty()) {
      service.fetchNotifications(session.accessToken, limit = pageSize, offset = 0)
        .onSuccess { list ->
          AdminNotificationDispatcher.dispatchUnseen(getApplication(), list)
          _state.update { it.copy(items = list, hasMore = list.size >= pageSize) }
        }
    }
  }

  fun loadMore(session: AdminSession?) {
    if (session == null || session.accessToken.isBlank() || _state.value.isLoadingMore) return
    val offset = _state.value.items.size
    viewModelScope.launch {
      _state.update { it.copy(isLoadingMore = true) }
      service.fetchNotifications(session.accessToken, limit = pageSize, offset = offset).fold(
        onSuccess = { more ->
          val merged = (_state.value.items + more).distinctBy { it.id }
          _state.update { it.copy(items = merged, isLoadingMore = false, hasMore = more.size >= pageSize) }
        },
        onFailure = { e -> _state.update { it.copy(isLoadingMore = false, errorMessage = e.message) } }
      )
    }
  }

  fun markAllAsRead(session: AdminSession?) {
    if (session == null || session.accessToken.isBlank()) return
    viewModelScope.launch {
      service.markRead(null, session.accessToken).fold(
        onSuccess = {
          val now = java.time.Instant.now().toString()
          _state.update { s ->
            s.copy(
              unreadCount = 0,
              items = s.items.map { if (it.isUnread) it.copy(readAt = now) else it }
            )
          }
        },
        onFailure = { e -> _state.update { it.copy(errorMessage = e.message) } }
      )
    }
  }

  fun onNotificationClick(item: AdminNotificationItem, session: AdminSession?) {
    if (session != null && item.isUnread) {
      viewModelScope.launch {
        service.markRead(listOf(item.id), session.accessToken)
        val now = java.time.Instant.now().toString()
        _state.update { s ->
          val updated = s.items.map { if (it.id == item.id) it.copy(readAt = now) else it }
          s.copy(items = updated, unreadCount = (s.unreadCount - 1).coerceAtLeast(0))
        }
      }
    }
    _state.update {
      it.copy(pendingDeepLink = AdminNotificationDeepLink(item.kind, item.targetClassId, item.data))
    }
  }

  fun triggerDeepLink(link: AdminNotificationDeepLink, session: AdminSession?) {
    val notifId = link.data["notification_id"]
    if (!notifId.isNullOrBlank() && session != null) {
      viewModelScope.launch { service.markRead(listOf(notifId), session.accessToken) }
    }
    _state.update { it.copy(pendingDeepLink = link) }
  }

  fun consumeDeepLink() = _state.update { it.copy(pendingDeepLink = null) }
  fun clearError() = _state.update { it.copy(errorMessage = null) }

  fun stopAndClear() {
    pollJob?.cancel()
    pollJob = null
    AdminNotificationWorker.cancel(getApplication())
    AdminNotificationPrefs.clearOnLogout(getApplication())
    _state.value = AdminNotificationUiState()
  }
}
