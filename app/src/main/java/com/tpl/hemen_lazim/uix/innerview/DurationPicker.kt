package com.tpl.hemen_lazim.uix.innerview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tpl.hemen_lazim.model.RequestLimits
import com.tpl.hemen_lazim.utils.formatDuration
import com.tpl.hemen_lazim.utils.formatEndTime
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private val QUICK_PICKS = listOf(30, 60, 3 * 60, 24 * 60)
private val HOURS = (0..RequestLimits.MAX_DURATION_MIN / 60).toList()
private val MINUTE_STEPS = listOf(0, 15, 30, 45)

private val WHEEL_ITEM_HEIGHT = 40.dp
private const val WHEEL_VISIBLE_ITEMS = 3

/**
 * Talebin açık kalacağı süre: hızlı seçim çipleri ve saat / dakika tekerlekleri.
 *
 * Tek doğru kaynak [minutes]; çipler ve tekerlekler ondan türetilir. Sınır dışı bir
 * birleşim (0 sa 00 dk, 24 sa 30 dk) en yakın geçerli değere çekilir ve tekerlek ona döner.
 */
@Composable
fun DurationPicker(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hours = minutes / 60
    val minutePart = minutes % 60

    Column(modifier.fillMaxWidth()) {
        Text(text = "Ne kadar süre açık kalsın?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QUICK_PICKS.forEach { quick ->
                FilterChip(
                    selected = minutes == quick,
                    onClick = { onMinutesChange(quick) },
                    label = { Text(formatDuration(quick)) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(
                values = HOURS,
                selected = hours,
                label = { "$it sa" },
                description = "Saat",
                onSelected = { h -> onMinutesChange(clampDuration(h * 60 + minutePart)) }
            )
            WheelPicker(
                values = MINUTE_STEPS,
                selected = minutePart,
                label = { "%02d dk".format(it) },
                description = "Dakika",
                onSelected = { m -> onMinutesChange(clampDuration(hours * 60 + m)) }
            )
        }
        Spacer(Modifier.height(8.dp))

        Text(
            text = "Bitiş: ${formatEndTime(minutes)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun clampDuration(minutes: Int) =
    minutes.coerceIn(RequestLimits.MIN_DURATION_MIN, RequestLimits.MAX_DURATION_MIN)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> WheelPicker(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    description: String,
    onSelected: (T) -> Unit
) {
    val selectedIndex = values.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val scope = rememberCoroutineScope()
    val currentOnSelected by rememberUpdatedState(onSelected)
    val itemHeightPx = with(LocalDensity.current) { WHEEL_ITEM_HEIGHT.toPx() }
    val centeredIndex by remember { derivedStateOf { listState.centeredIndex(itemHeightPx) } }
    // Tekerlek her durduğunda artar: seçim sınıra çekildiği için değişmediyse de tekerlek
    // geçerli değere geri dönebilsin
    var settleCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { inProgress -> !inProgress }
            .collect {
                currentOnSelected(values[listState.centeredIndex(itemHeightPx).coerceIn(values.indices)])
                settleCount++
            }
    }

    // Dışarıdan gelen değişiklik (çip ya da sınıra çekme): tekerleği değere getir
    LaunchedEffect(selectedIndex, settleCount) {
        if (listState.centeredIndex(itemHeightPx) != selectedIndex) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Box(
        modifier = Modifier
            .width(110.dp)
            .height(WHEEL_ITEM_HEIGHT * WHEEL_VISIBLE_ITEMS)
            .semantics { contentDescription = "$description: ${label(selected)}" },
        contentAlignment = Alignment.Center
    ) {
        // Ortadaki (seçili) satırın vurgusu
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(WHEEL_ITEM_HEIGHT),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {}

        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            // Bir satırlık boşluk: ilk ve son değer de ortaya gelebilsin
            contentPadding = PaddingValues(vertical = WHEEL_ITEM_HEIGHT),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            items(values.size) { index ->
                val isCentered = index == centeredIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WHEEL_ITEM_HEIGHT)
                        .clickable { scope.launch { listState.animateScrollToItem(index) } },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label(values[index]),
                        style = if (isCentered) MaterialTheme.typography.titleMedium
                                else MaterialTheme.typography.bodyMedium,
                        color = if (isCentered) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// contentPadding bir satır yüksekliğinde: ilk görünen öğe tam ortadaki satırdadır
private fun LazyListState.centeredIndex(itemHeightPx: Float): Int =
    firstVisibleItemIndex + if (firstVisibleItemScrollOffset > itemHeightPx / 2) 1 else 0
