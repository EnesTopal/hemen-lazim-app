package com.tpl.hemen_lazim.network.repositories


import android.util.Log
import com.tpl.hemen_lazim.network.RetrofitClient
import com.tpl.hemen_lazim.network.services.HealthCheckService


class HealthCheckRepository {
    private val service = RetrofitClient.retrofit.create(HealthCheckService::class.java)

    suspend fun checkServerHealth() : String {
        try {
            val response = service.checkHealth()
            Log.d("HealthCheck", "Genel Durum: ${response.status}")

            response.components?.let { components ->
                components.db?.let { db ->
                    Log.d("HealthCheck", "DB Durumu: ${db.status}")
                    db.details?.forEach { (key, value) ->
                        Log.d("HealthCheck", "DB Detay - $key: $value")
                    }
                }

                components.diskSpace?.let { disk ->
                    Log.d("HealthCheck", "Disk Durumu: ${disk.status}")
                }

                components.ping?.let { ping ->
                    Log.d("HealthCheck", "Ping Durumu: ${ping.status}")
                }

                components.ssl?.let { ssl ->
                    Log.d("HealthCheck", "SSL Durumu: ${ssl.status}")
                }
            }
            return response.status
        } catch (e: Exception) {
            Log.e("HealthCheck", "Bağlantı hatası: ${e.message}")
            return "Bağlantı hatası: ${e.message}"
        }
    }

}
