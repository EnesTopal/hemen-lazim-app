package com.tpl.hemen_lazim.network

import com.tpl.hemen_lazim.model.DTOs.ApiResponse
import com.tpl.hemen_lazim.model.DTOs.RefreshRequest
import com.tpl.hemen_lazim.model.DTOs.TokenResponse
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TokenAuthenticator(
    private val baseUrl: String
) : Authenticator {

    // Refresh için "temiz" bir Retrofit: interceptor/authenticator eklemiyoruz (sonsuz döngü olmasın)
    private val plainClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val refreshRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(plainClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Sadece bu sınıfın içinde kullanılan minimal API
    private interface RefreshApi {
        @POST("auth/refresh")
        fun refresh(@Body body: RefreshRequest): Call<ApiResponse<TokenResponse>>
    }

    private val refreshApi: RefreshApi by lazy { refreshRetrofit.create(RefreshApi::class.java) }

    // Eş zamanlı çoklu 401 durumunda tek refresh çalışsın diye kilit
    private val lock = ReentrantLock()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Sonsuz döngüyü önle: aynı isteği tekrar tekrar deneme
        if (responseCount(response) >= 2) return null

        val refreshToken = SharedPreferencesProvider.getRefreshToken() ?: return null

        // Kilit içinde: sadece bir thread refresh yapsın
        return lock.withLock {
            // Bu esnada başka bir thread refresh yapmış olabilir; güncel access var mı?
            val currentAccess = SharedPreferencesProvider.getAccessToken()
            if (!currentAccess.isNullOrBlank()) {
                val reqHeader = response.request.header("Authorization")
                if (reqHeader != null && reqHeader == "Bearer $currentAccess") {
                    // gerçekten yenilememiz gerekiyor; devam et
                } else {
                    // Başkası zaten yenilemiş → yeni access ile tekrar dene
                    return@withLock newRequestWithAccess(response.request, currentAccess)
                }
            }

            // Refresh’i bloklayıcı şekilde yap
            val call = refreshApi.refresh(RefreshRequest(refreshToken))
            val res = runCatching { call.execute() }.getOrNull()

            if (res?.isSuccessful == true) {
                val body = res.body()?.data
                if (body?.accessToken.isNullOrBlank() || body?.refreshToken.isNullOrBlank()) {
                    // Geçersiz dönüş → session’ı temizle
                    SharedPreferencesProvider.clearSession()
                    return@withLock null
                }

                // Yeni oturumu kaydet
                SharedPreferencesProvider.saveSession(
                    accessToken = body!!.accessToken,
                    refreshToken = body.refreshToken
                )

                // Orijinal isteği yeni access ile yeniden gönder
                return@withLock newRequestWithAccess(response.request, body.accessToken)
            } else {
                // Refresh başarısız → session’ı temizle, 401 üst katmana gitsin
                SharedPreferencesProvider.clearSession()
                return@withLock null
            }
        }
    }

    private fun newRequestWithAccess(original: Request, access: String): Request {
        return original.newBuilder()
            .removeHeader("Authorization")
            .addHeader("Authorization", "Bearer $access")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior: Response? = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
