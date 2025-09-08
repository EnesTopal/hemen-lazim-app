package com.tpl.hemen_lazim.network.repositories

import android.util.Log
import com.tpl.hemen_lazim.model.DTOs.*
import com.tpl.hemen_lazim.network.services.AuthService
import com.tpl.hemen_lazim.network.RetrofitClient
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(
    private val api: AuthService = RetrofitClient.retrofit.create(AuthService::class.java)
) {

    suspend fun login(createUserDTO: CreateUserDTO): Result<Unit> {
        return try {
            val res = api.login(createUserDTO)
            if (res.isSuccessful) {
                val tr = res.body()?.data
                if (tr != null && tr.accessToken.isNotBlank() && tr.refreshToken.isNotBlank()) {
                    SharedPreferencesProvider.saveSession(tr.accessToken, tr.refreshToken)
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("Boş token(lar) döndü"))
                }
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

    suspend fun register(createUserDTO: CreateUserDTO): Result<UserDTO> {
        return try {
            val res = api.register(createUserDTO)
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

    suspend fun refresh(refreshToken: String): Result<Unit> {
        return try {
            val res = api.refresh(RefreshRequest(refreshToken))
            if (res.isSuccessful) {
                val tr = res.body()?.data
                if (tr != null && tr.accessToken.isNotBlank() && tr.refreshToken.isNotBlank()) {
                    SharedPreferencesProvider.saveSession(tr.accessToken, tr.refreshToken)
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("Boş token(lar) döndü"))
                }
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


    suspend fun logout(): Result<Unit> {
        val refresh = SharedPreferencesProvider.getRefreshToken()
            ?: return Result.success(Unit).also { SharedPreferencesProvider.clearSession() }

        return try {
            val res = api.logout(RefreshRequest(refresh))
            // Başarılı ya da değil — local session'ı temizle
            SharedPreferencesProvider.clearSession()
            if (res.isSuccessful) Result.success(Unit)
            else Result.failure(IllegalStateException(extractApiMessage(res.errorBody()?.string())))
        } catch (_: Exception) {
            SharedPreferencesProvider.clearSession()
            Result.success(Unit)
        }
    }

    private fun extractApiMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return "Bir hata oluştu."
        return try {
            JSONObject(raw).optString("message").ifBlank { "Bir hata oluştu." }
        } catch (_: Exception) { raw }
    }
}
