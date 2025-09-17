package com.tpl.hemen_lazim.uix.innerview

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SegButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(14.dp)
    val border =
        if (selected) ButtonDefaults.outlinedButtonColors() else ButtonDefaults.outlinedButtonColors()
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        modifier = Modifier.height(40.dp)
    ) { Text(text) }
}