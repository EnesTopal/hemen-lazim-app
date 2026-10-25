package com.tpl.hemen_lazim.uix.innerview

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tpl.hemen_lazim.R

/** Keşfet ve İlanlarım'daki "+ Talep aç": talep açmak bir sekme değil, her yerden yapılan eylem. */
@Composable
fun CreateRequestFab(onClick: () -> Unit, expanded: Boolean = true) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        expanded = expanded,
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text(stringResource(R.string.create_request)) }
    )
}
