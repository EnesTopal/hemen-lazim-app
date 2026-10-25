package com.tpl.hemen_lazim.uix.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.model.RequestLimits
import com.tpl.hemen_lazim.network.repositories.ChatRepository
import com.tpl.hemen_lazim.network.repositories.NotificationRepository
import com.tpl.hemen_lazim.network.repositories.RequestRepository
import com.tpl.hemen_lazim.ui.messageRes
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class RespondUiState(
        val radiusKm: Float = RequestLimits.DEFAULT_SEARCH_RADIUS_KM,
        val userLatitude: Double? = null,
        val userLongitude: Double? = null,
        val nearbyRequests: List<MaterialRequestDTO> = emptyList(),
        val isLoading: Boolean = false,
        // En az bir arama tamamlandı mı: "henüz aranmadı" ile "bu alanda talep yok" ayrışsın
        val hasLoaded: Boolean = false,
        // Son arama başarısızsa nedeni; eski liste ekranda kalır
        @StringRes val loadError: Int? = null,
        @StringRes val toastMessage: Int? = null,
        val selectedRequest: MaterialRequestDTO? = null,
        // Son dokunulan talep: detay kapandıktan sonra da dairesi haritada kalır
        val highlightedRequest: MaterialRequestDTO? = null,
        val isSendingNotification: Boolean = false
)

class RespondViewModel(
        private val repo: RequestRepository = RequestRepository(),
        private val notificationRepo: NotificationRepository = NotificationRepository(),
        private val chatRepo: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(RespondUiState())
    val ui = _ui.asStateFlow()

    fun setUserLocation(latitude: Double, longitude: Double) {
        _ui.update { it.copy(userLatitude = latitude, userLongitude = longitude) }
        fetchNearbyRequests()
    }

    // Sürüklerken yalnızca daire güncellenir; istek kaydırıcı bırakılınca atılır
    fun setRadius(radiusKm: Float) {
        _ui.update { it.copy(radiusKm = radiusKm) }
    }

    /** Boş sonuçta "Alanı genişlet": yarıçapı ikiye katlar (üst sınıra kadar) ve yeniden arar. */
    fun expandRadius() {
        _ui.update { it.copy(radiusKm = (it.radiusKm * 2).coerceAtMost(RequestLimits.MAX_RADIUS_KM)) }
        fetchNearbyRequests()
    }

    fun selectRequest(request: MaterialRequestDTO?) {
        _ui.update {
            it.copy(selectedRequest = request, highlightedRequest = request ?: it.highlightedRequest)
        }
    }

    fun clearToast() {
        _ui.update { it.copy(toastMessage = null) }
    }

    // Yalnızca son istek sonucu yazar: konum ya da yarıçap hızlı değişince öncekiler iptal edilir,
    // geç gelen eski bir yanıt yenisinin üstüne yazamaz
    private var fetchJob: Job? = null

    fun fetchNearbyRequests() {
        fetchJob?.cancel()
        fetchJob =
                viewModelScope.launch {
                    val state = _ui.value
                    if (state.userLatitude == null || state.userLongitude == null) {
                        return@launch
                    }

                    _ui.update { it.copy(isLoading = true, loadError = null) }

                    val radiusMeters = (state.radiusKm * 1000).roundToInt()
                    val result =
                            repo.listNearby(
                                    lat = state.userLatitude,
                                    lng = state.userLongitude,
                                    radius = radiusMeters,
                                    category = null // Search all categories
                            )
                    // Yeni bir arama bu isteği iptal ettiyse eski sonuç yazılmasın
                    ensureActive()

                    if (result.isSuccess) {
                        val requests = result.getOrNull() ?: emptyList()
                        _ui.update {
                            it.copy(
                                    nearbyRequests = requests,
                                    // Artık listede olmayan (süresi dolan) talebin dairesi kalmasın
                                    highlightedRequest =
                                            it.highlightedRequest?.takeIf { highlighted ->
                                                requests.any { r -> r.id == highlighted.id }
                                            },
                                    isLoading = false,
                                    hasLoaded = true
                            )
                        }
                    } else {
                        // Hata listenin yerinde "Tekrar dene" ile gösterilir, toast değil
                        _ui.update {
                            it.copy(
                                    isLoading = false,
                                    loadError = result.exceptionOrNull()?.messageRes()
                                                    ?: R.string.error_unknown
                            )
                        }
                    }
                }
    }

    fun sendSupplyOfferNotification(requestId: String, requesterId: String, onSuccess: () -> Unit) =
            viewModelScope.launch {
                _ui.update { it.copy(isSendingNotification = true) }

                val result = notificationRepo.sendSupplyOfferNotification(requestId, requesterId)

                result
                        .onSuccess { notifiedNow ->
                            // Aynı talebe tekrar basınca sunucu ikinci bir bildirim göndermiyor
                            val message =
                                    if (notifiedNow) R.string.offer_sent else R.string.offer_already_sent
                            _ui.update { it.copy(isSendingNotification = false, toastMessage = message) }
                            onSuccess()
                        }
                        .onFailure { e ->
                            _ui.update {
                                it.copy(isSendingNotification = false, toastMessage = e.messageRes())
                            }
                        }
            }

    fun createChatRoom(senderId: String, recipientId: String, onSuccess: (String) -> Unit) =
            viewModelScope.launch {
                try {
                    val roomId = chatRepo.createRoom(senderId, recipientId)
                    onSuccess(roomId)
                } catch (e: Exception) {
                    _ui.update { it.copy(toastMessage = e.messageRes()) }
                }
            }
}
