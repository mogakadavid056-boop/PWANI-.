package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ClassSearchUiState(
  val searchQuery: String = "",
  val selectedSchool: SchoolItem? = null,
  val selectedYear: Int? = null,
  val onlyWithoutPrefect: Boolean = false,
  val results: List<SearchClassItem> = emptyList(),
  val schools: List<SchoolItem> = emptyList(),
  val isLoading: Boolean = false,
  val isClaimingId: String? = null,
  val canLoadMore: Boolean = false,
  val offsetRows: Int = 0,
  val errorMessage: String? = null,
  val successMessage: String? = null
)

@OptIn(FlowPreview::class)
class ClassSearchViewModel(
  private val searchService: ClassSearchService = ClassSearchService(),
  private val universityService: UniversityService = UniversityService()
) : ViewModel() {

  private val _uiState = MutableStateFlow(ClassSearchUiState())
  val uiState: StateFlow<ClassSearchUiState> = _uiState.asStateFlow()

  private val _searchQueryFlow = MutableStateFlow("")
  private var activeSession: UserSession? = null
  private var searchJob: Job? = null

  init {
    viewModelScope.launch {
      _searchQueryFlow
        .debounce(400L)
        .distinctUntilChanged()
        .collectLatest { query ->
          _uiState.update { it.copy(searchQuery = query) }
          performSearch(isNewSearch = true)
        }
    }
  }

  fun setSession(session: UserSession?) {
    val previousSession = activeSession
    activeSession = session
    if (session != null && (previousSession?.userId != session.userId || _uiState.value.schools.isEmpty())) {
      loadSchools(session.accessToken)
      performSearch(isNewSearch = true)
    }
  }

  fun onSearchQueryChanged(query: String) {
    _searchQueryFlow.value = query
  }

  fun onSchoolFilterChanged(school: SchoolItem?) {
    _uiState.update { it.copy(selectedSchool = school) }
    performSearch(isNewSearch = true)
  }

  fun onYearFilterChanged(year: Int?) {
    _uiState.update { it.copy(selectedYear = year) }
    performSearch(isNewSearch = true)
  }

  fun onWithoutPrefectToggled(enabled: Boolean) {
    _uiState.update { it.copy(onlyWithoutPrefect = enabled) }
    performSearch(isNewSearch = true)
  }

  fun loadSchools(token: String?) {
    viewModelScope.launch {
      val res = universityService.fetchSchools(token)
      res.fold(
        onSuccess = { list -> _uiState.update { it.copy(schools = list) } },
        onFailure = { /* keep default */ }
      )
    }
  }

  fun refresh() {
    performSearch(isNewSearch = true)
  }

  fun loadMore() {
    if (_uiState.value.isLoading || !_uiState.value.canLoadMore) return
    performSearch(isNewSearch = false)
  }

  private fun performSearch(isNewSearch: Boolean) {
    val session = activeSession ?: return
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      val currentOffset = if (isNewSearch) 0 else _uiState.value.offsetRows + 30
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }

      val state = _uiState.value
      val result = searchService.searchClasses(
        searchText = state.searchQuery.trim(),
        schoolFilter = state.selectedSchool?.id,
        yearFilter = state.selectedYear,
        onlyWithoutPrefect = state.onlyWithoutPrefect,
        maxRows = 30,
        offsetRows = currentOffset,
        accessToken = session.accessToken
      )

      result.fold(
        onSuccess = { list ->
          val combined = if (isNewSearch) list else state.results + list
          _uiState.update {
            it.copy(
              results = combined,
              offsetRows = currentOffset,
              canLoadMore = list.size >= 30,
              isLoading = false
            )
          }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = err.message ?: "Failed to search classes"
            )
          }
        }
      )
    }
  }

  fun claimClass(
    item: SearchClassItem,
    onSuccess: (ClassItem) -> Unit
  ) {
    val session = activeSession ?: return
    _uiState.update { it.copy(isClaimingId = item.id, errorMessage = null) }

    viewModelScope.launch {
      val result = searchService.claimClass(item.id, session.accessToken)
      result.fold(
        onSuccess = {
          _uiState.update {
            val updatedList = it.results.map { c -> if (c.id == item.id) c.copy(isMine = true) else c }
            it.copy(isClaimingId = null, results = updatedList, successMessage = "Class added to your classes")
          }
          val classItem = ClassItem(
            id = item.id,
            name = item.className,
            code = item.classCode,
            programmeName = item.programmeName
          )
          onSuccess(classItem)
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isClaimingId = null,
              errorMessage = err.message ?: "Failed to add class"
            )
          }
        }
      )
    }
  }

  fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
  fun clearSuccess() { _uiState.update { it.copy(successMessage = null) } }
}
