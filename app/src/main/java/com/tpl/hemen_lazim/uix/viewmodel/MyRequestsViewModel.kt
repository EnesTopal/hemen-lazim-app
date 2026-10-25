package com.tpl.hemen_lazim.uix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.network.repositories.RequestRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyRequestsUiState(
    val requests: List<MaterialRequestDTO> = emptyList(),
    val isLoading: Boolean = false,
    val loadError: String? = null
)

class MyRequestsViewModel(
    private val repo: RequestRepository = RequestRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(MyRequestsUiState())
    val ui: StateFlow<MyRequestsUiState> = _ui.asStateFlow()

    private var loadJob: Job? = null

    /** Sekmeye her dönüşte çağrılır; üst üste gelirse yalnızca sonuncusu ekrana yazar. */
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, loadError = null) }
            repo.listMine()
                .onSuccess { list -> _ui.update { it.copy(requests = list, isLoading = false) } }
                .onFailure { e -> _ui.update { it.copy(isLoading = false, loadError = e.message) } }
        }
    }
}
