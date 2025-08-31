package com.tpl.hemen_lazim.network

import com.tpl.hemen_lazim.utils.SharedPreferencesProvider
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = SharedPreferencesProvider.getToken()
        val requestBuilder: Request.Builder = chain.request().newBuilder()

        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
