package com.tpl.hemen_lazim.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tpl.hemen_lazim.MainActivity
import com.tpl.hemen_lazim.R
import com.tpl.hemen_lazim.utils.SharedPreferencesProvider
import com.tpl.hemen_lazim.network.RetrofitClient
import com.tpl.hemen_lazim.network.services.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.random.Random

class HemenLazimFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "hemen_lazim_notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        
        // Initialize SharedPreferences
        SharedPreferencesProvider.init(this)
        
        // Save token locally
        SharedPreferencesProvider.saveFcmToken(token)
        
        // Send token to server if user is logged in
        if (SharedPreferencesProvider.getAccessToken() != null) {
            sendTokenToServer(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        var title = "Hemen Lazım"
        var body = ""
        
        // Check if message contains a notification payload
        message.notification?.let {
            Log.d(TAG, "Message Notification Title: ${it.title}")
            Log.d(TAG, "Message Notification Body: ${it.body}")
            title = it.title ?: title
            body = it.body ?: ""
        }

        // Check if message contains a data payload
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${message.data}")
            
            // If no notification payload, get from data
            if (message.notification == null) {
                title = message.data["title"] ?: title
                body = message.data["body"] ?: body
            }
        }
        
        // Always show notification (works even when app is closed)
        if (body.isNotEmpty()) {
            showNotification(title, body, message.data)
        } else {
            Log.w(TAG, "Notification body is empty, not showing notification")
        }
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        Log.d(TAG, "Showing notification: $title - $body")
        
        // Create notification channel first
        createNotificationChannel()

        // Create intent for notification tap
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Add data to intent if needed
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            Random.nextInt(), // Use random ID for unique pending intents
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Get default notification sound
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Build notification with maximum priority for heads-up display
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX) // Changed to MAX for heads-up
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(defaultSoundUri) // Add sound explicitly
            .setVibrate(longArrayOf(0, 500, 200, 500)) // Vibration pattern
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(false) // Always alert
            .setFullScreenIntent(null, true) // Enable heads-up
            .build()

        // Use unique notification ID so multiple notifications can show
        val notificationId = System.currentTimeMillis().toInt()
        
        try {
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "✅ Notification displayed with ID: $notificationId")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Notification permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error displaying notification", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Tedarik Teklifleri",
                NotificationManager.IMPORTANCE_HIGH // HIGH for heads-up notifications
            ).apply {
                description = "Malzeme talepleri için bildirimler"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                setShowBadge(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created with IMPORTANCE_HIGH")
        }
    }

    private fun sendTokenToServer(token: String) {
        serviceScope.launch {
            try {
                val service = RetrofitClient.retrofit.create(NotificationService::class.java)
                val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                val request = com.tpl.hemen_lazim.model.DTOs.DeviceRegisterRequestDTO(
                    fcmToken = token,
                    deviceType = "ANDROID",
                    deviceName = deviceName
                )
                val response = service.registerDevice(request)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Device registered successfully")
                } else {
                    Log.e(TAG, "Failed to register device: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering device", e)
            }
        }
    }
}
