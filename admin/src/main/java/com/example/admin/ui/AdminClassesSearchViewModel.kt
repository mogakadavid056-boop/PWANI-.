package com.example.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.admin.data.AdminClassSearchRepository
import com.example.admin.data.AdminSearchClassItem
import com.example.admin.data.AdminSession
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminClassesSearchState(
  val items: List<AdminSearchClassItem> = emptyList(),
  val searchText: String = "",
  val schoolFilter: String? = null,
  val yearFilter: Int? = null,
  val onlyWithoutPrefect: Boolean = false,
  val hasMore: Boolean = false,
  val isLoading: Boolean = false,
  val isLoadingMore: Boolean = false,
  val errorMessage: String? = null
)

@OptIn(FlowPreview::class)
class AdminClassesSearchViewModel(
  private val repo: AdminClassSearchRepository = AdminClassSearchRepository()
) : ViewModel() {
  private val _state = MutableStateFlow(AdminClassesSearchState())
  val state: StateFlow<AdminClassesSearchState> = _state.asStateFlow()

  private val searchFlow = MutableStateFlow("")
  private var session: AdminSession? = null
  private val pageSize = 30

  init {
    viewModelScope.launch {
      searchFlow.debounce(400L).distinctUntilChanged().collectLatest {
        search(reset = true)
      }
    }
  }

  fun setSession(newSession: AdminSession?) {
    val changed = session?.userId != newSession?.userId
    session = newSession
    if (newSession != null && changed) search(reset = true) else if (newSession == null) _state.value = AdminClassesSearchState()
  }

  fun onSearchChanged(q: String) {
    _state.update { it.copy(searchText = q) }
    searchFlow.value = q
  }

  fun setSchoolFilter(schoolName: String?) {
    _state.update { it.copy(schoolFilter = schoolName?.ifBlank { null }) }
    search(reset = true)
  }

  fun setYearFilter(year: Int?) {
    _state.update { it.copy(yearFilter = year) }
    search(reset = true)
  }

  fun setOnlyWithoutPrefect(only: Boolean) {
    _state.update { it.copy(onlyWithoutPrefect = only) }
    search(reset = true)
  }

  fun search(reset: Boolean = true) {
    val token = session?.accessToken ?: return
    val s = _state.value
    val offset = if (reset) 0 else s.items.size
    viewModelScope.launch {
      _state.update { it.copy(isLoading = reset, isLoadingMore = !reset, errorMessage = null) }
      repo.searchClasses(s.searchText, s.schoolFilter, s.yearFilter, s.onlyWithoutPrefect, pageSize, offset, token).fold(
        onSuccess = { list ->
          val merged = if (reset) list else (s.items + list).distinctBy { it.id }
          _state.update { it.copy(items = merged, hasMore = list.size >= pageSize, isLoading = false, isLoadingMore = false) }
        },
        onFailure = { e -> _state.update { it.copy(isLoading = false, isLoadingMore = false, errorMessage = e.message) } }
      )
    }
  }

  fun clearError() = _state.update { it.copy(errorMessage = null) }
}
