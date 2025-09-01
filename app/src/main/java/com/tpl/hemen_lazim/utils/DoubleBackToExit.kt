package com.tpl.hemen_lazim.utils

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun DoubleBackToExit() {
    var backPressedOnce by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val handler = Handler(Looper.getMainLooper())

    BackHandler(enabled = true) {
        if (backPressedOnce) {
            (context as? Activity)?.finish()
        } else {
            backPressedOnce = true
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            // Reset the backPressedOnce flag after 2 seconds
            handler.postDelayed({
                backPressedOnce = false
            }, 2000)
        }
    }
}