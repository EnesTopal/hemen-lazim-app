package com.tpl.hemen_lazim.network.repositories

import com.tpl.hemen_lazim.model.DTOs.CreateUserDTO
import com.tpl.hemen_lazim.model.DTOs.UserDTO
import com.tpl.hemen_lazim.network.services.AuthService
import com.tpl.hemen_lazim.network.RetrofitClient
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(
    private val api: AuthService = RetrofitClient.retrofit.create(AuthService::class.java)
) {

    suspend fun login(username: String, password: String): Result<String> {
        return try {
            val res = api.login(CreateUserDTO(username = username, userpassword = password))
            if (res.isSuccessful) {
                val token = res.body()?.data
                if (!token.isNullOrEmpty()) Result.success(token)
                else Result.failure(IllegalStateException("Boş token döndü"))
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

    suspend fun register(username: String, password: String): Result<UserDTO> {
        return try {
            val res = api.register(CreateUserDTO(username = username, userpassword = password))
            if (res.isSuccessful) {
                val user = res.body()?.data
                if (user != null) Result.success(user)
                else Result.failure(IllegalStateException("Kullanıcı bilgisi boş"))
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

    // Backend'in ApiResponse {"message": "...", "data": ...} hatasından mesaj çekme
    private fun extractApiMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return "Bir hata oluştu."
        return try {
            JSONObject(raw).optString("message").ifBlank { "Bir hata oluştu." }
        } catch (_: Exception) {
            raw
        }
    }
}
