package com.tpl.hemen_lazim.uix.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.model.enums.RequestStatus
import com.tpl.hemen_lazim.network.repositories.RequestRepository
import com.tpl.hemen_lazim.ui.messageRes
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyRequestsUiState(
    val requests: List<MaterialRequestDTO> = emptyList(),
    val isLoading: Boolean = false,
    @StringRes val loadError: Int? = null,
    // Üzerinde işlem süren talepler: butonları kapalı, yanında gösterge
    val busyIds: Set<String> = emptySet(),
    // Tek seferlik geri bildirim (Snackbar)
    @StringRes val message: Int? = null
)

class MyRequestsViewModel(
    private val repo: RequestRepository = RequestRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(MyRequestsUiState())
    val ui: StateFlow<MyRequestsUiState> = _ui.asStateFlow()

    private var loadJob: Job? = null

    /** Sekmeye her dönüşte ve aşağı çekince çağrılır; üst üste gelirse yalnızca sonuncusu yazar. */
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, loadError = null) }
            repo.listMine()
                .onSuccess { list -> _ui.update { it.copy(requests = list, isLoading = false) } }
                .onFailure { e -> _ui.update { it.copy(isLoading = false, loadError = e.messageRes()) } }
        }
    }

    fun complete(id: String) =
        runAction(id, RequestStatus.COMPLETED, R.string.request_completed) { repo.complete(id) }

    fun cancel(id: String) =
        runAction(id, RequestStatus.CANCELLED, R.string.request_cancelled) { repo.cancel(id) }

    fun extend(id: String, minutes: Int) {
        if (id in _ui.value.busyIds) return
        viewModelScope.launch {
            _ui.update { it.copy(busyIds = it.busyIds + id) }
            repo.extend(id, minutes)
                .onSuccess { updated ->
                    _ui.update { s ->
                        s.copy(
                            requests = s.requests.map { if (it.id == id) updated else it },
                            message = R.string.request_extended
                        )
                    }
                }
                .onFailure { e -> onActionFailed(e) }
            _ui.update { it.copy(busyIds = it.busyIds - id) }
        }
    }

    fun clearMessage() = _ui.update { it.copy(message = null) }

    private fun runAction(
        id: String,
        newStatus: RequestStatus,
        @StringRes successMessage: Int,
        call: suspend () -> Result<*>
    ) {
        if (id in _ui.value.busyIds) return
        viewModelScope.launch {
            _ui.update { it.copy(busyIds = it.busyIds + id) }
            call()
                .onSuccess {
                    // Sunucu yalnızca mesaj dönüyor; listeyi beklemeden yerinde güncelle
                    _ui.update { s ->
                        s.copy(
                            requests = s.requests.map { if (it.id == id) it.copy(status = newStatus) else it },
                            message = successMessage
                        )
                    }
                }
                .onFailure { e -> onActionFailed(e) }
            _ui.update { it.copy(busyIds = it.busyIds - id) }
        }
    }

    // Çoğunlukla talep bu arada kapanmıştır (süresi doldu): mesajı göster, listeyi tazele
    private fun onActionFailed(e: Throwable) {
        _ui.update { it.copy(message = e.messageRes()) }
        refresh()
    }
}
