package com.tpl.hemen_lazim.uix.innerview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import com.tpl.hemen_lazim.model.RequestLimits
import com.tpl.hemen_lazim.utils.formatRadiusKm
import kotlin.math.sqrt

/**
 * İlan açma ve arama ekranlarının ortak yarıçap kaydırıcısı.
 *
 * Ölçek kareseldir: kaydırıcının ilk yarısı ~0,1–2,6 km'yi kapsar. Talepler çoğunlukla
 * birkaç km içinde olduğundan yakın mesafede ince ayar, 10 km'ye kadar doğrusal ölçekten
 * daha kullanışlı.
 */
@Composable
fun RadiusSlider(
    title: String,
    radiusKm: Float,
    onRadiusChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    // Sürükleme bitince bir kez çağrılır: ağ isteği gibi pahalı işler buraya
    onRadiusChangeFinished: () -> Unit = {},
    minKm: Float = RequestLimits.MIN_RADIUS_KM,
    maxKm: Float = RequestLimits.MAX_RADIUS_KM
) {
    val span = maxKm - minKm
    val label = formatRadiusKm(radiusKm)

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = sqrt(((radiusKm - minKm) / span).coerceIn(0f, 1f)),
            onValueChange = { position ->
                onRadiusChange((minKm + span * position * position).coerceIn(minKm, maxKm))
            },
            onValueChangeFinished = onRadiusChangeFinished,
            // Ölçek doğrusal olmadığı için varsayılan "yüzde" okuması yanıltıcı olurdu
            modifier = Modifier.semantics {
                contentDescription = title
                stateDescription = label
            }
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = formatRadiusKm(minKm),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatRadiusKm(maxKm),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
