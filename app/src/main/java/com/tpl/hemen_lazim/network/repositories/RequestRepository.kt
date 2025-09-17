package com.tpl.hemen_lazim.network.repositories

import com.tpl.hemen_lazim.model.DTOs.MaterialRequestCreateDTO
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.model.enums.Category
import com.tpl.hemen_lazim.network.RetrofitClient
import com.tpl.hemen_lazim.network.services.AdminService
import com.tpl.hemen_lazim.network.services.RequestService
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class RequestRepository(
    private val api: RequestService = RetrofitClient.retrofit.create(RequestService::class.java),
    private val adminApi: AdminService = RetrofitClient.retrofit.create(AdminService::class.java)
) {

//    suspend fun createRequest(body: MaterialRequestCreateDTO): Result<MaterialRequestDTO> {
//        return try {
//            val res = api.create(body)
//            if (res.isSuccessful) {
//                val dto = res.body()?.data
//                if (dto != null) Result.success(dto)
//                else Result.failure(IllegalStateException("Boş yanıt"))
//            } else {
//                Result.failure(IllegalStateException(extractApiMessage(res.errorBody()?.string())))
//            }
//        } catch (e: IOException) {
//            Result.failure(IOException("Bağlantı hatası: ${e.message}", e))
//        } catch (e: HttpException) {
//            Result.failure(IllegalStateException("Sunucu hatası: ${e.code()}"))
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
suspend fun create(body: MaterialRequestCreateDTO): Result<Unit> {
    return try {
        val res = api.create(body)
        if (res.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException(res.errorBody()?.string() ?: "İstek oluşturulamadı"))
        }
    } catch (e: IOException) {
        Result.failure(IOException("Ağ hatası: ${e.message}", e))
    } catch (e: HttpException) {
        Result.failure(IllegalStateException("Sunucu hatası: ${e.code()}"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

    suspend fun listNearby(
        lat: Double,
        lng: Double,
        radius: Int? = null,
        category: Category? = null
    ): Result<List<MaterialRequestDTO>> {
        return try {
            val res = api.nearby(lat, lng, radius, category)
            if (res.isSuccessful) {
                Result.success(res.body()?.data.orEmpty())
            } else {
                Result.failure(IllegalStateException(extractApiMessage(res.errorBody()?.string())))
            }
        } catch (e: IOException) {
            Result.failure(IOException("Bağlantı hatası: ${e.message}", e))
        } catch (e: HttpException) {
            Result.failure(IllegalStateException("Sunucu hatası: ${e.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listMine(): Result<List<MaterialRequestDTO>> {
        return try {
            val res = api.mine()
            if (res.isSuccessful) {
                Result.success(res.body()?.data.orEmpty())
            } else {
                Result.failure(IllegalStateException(extractApiMessage(res.errorBody()?.string())))
            }
        } catch (e: IOException) {
            Result.failure(IOException("Bağlantı hatası: ${e.message}", e))
        } catch (e: HttpException) {
            Result.failure(IllegalStateException("Sunucu hatası: ${e.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancel(id: String): Result<String> {
        return try {
            val res = api.cancel(id)
            if (res.isSuccessful) {
                Result.success(res.body()?.message ?: "OK")
            } else {
                Result.failure(IllegalStateException(extractApiMessage(res.errorBody()?.string())))
            }
        } catch (e: IOException) {
            Result.failure(IOException("Bağlantı hatası: ${e.message}", e))
        } catch (e: HttpException) {
            Result.failure(IllegalStateException("Sunucu hatası: ${e.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun complete(id: String): Result<String> {
        return try {
            val res = api.complete(id)
            if (res.isSuccessful) {
                Result.success(res.body()?.message ?: "OK")
            } else {
                Result.failure(IllegalStateException(extractApiMessage(res.errorBody()?.string())))
            }
        } catch (e: IOException) {
            Result.failure(IOException("Bağlantı hatası: ${e.message}", e))
        } catch (e: HttpException) {
            Result.failure(IllegalStateException("Sunucu hatası: ${e.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminGetAll(): Result<List<MaterialRequestDTO>> {
        return try {
            val res = adminApi.getAllRequests()
            if (res.isSuccessful) {
                Result.success(res.body()?.data.orEmpty())
            } else {
                Result.failure(IllegalStateException(extractApiMessage(res.errorBody()?.string())))
            }
        } catch (e: IOException) {
            Result.failure(IOException("Bağlantı hatası: ${e.message}", e))
        } catch (e: HttpException) {
            Result.failure(IllegalStateException("Sunucu hatası: ${e.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractApiMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return "Bir hata oluştu."
        return try {
            JSONObject(raw).optString("message").ifBlank { "Bir hata oluştu." }
        } catch (_: Exception) {
            raw
        }
    }
}
