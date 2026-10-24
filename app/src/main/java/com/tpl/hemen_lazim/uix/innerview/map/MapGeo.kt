package com.tpl.hemen_lazim.uix.innerview.map

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil

/** Kamerayı bir daireye (ve varsa ek noktalara) sığdırmak için kutu. */
fun boundsAround(center: LatLng, radiusMeters: Double, points: List<LatLng> = emptyList()): LatLngBounds =
    LatLngBounds.builder().apply {
        // Dairenin dört yöndeki ucu kutuyu belirler
        for (heading in listOf(0.0, 90.0, 180.0, 270.0)) {
            include(SphericalUtil.computeOffset(center, radiusMeters, heading))
        }
        points.forEach(::include)
    }.build()

/** Noktaların hepsi birbirine [meters]'dan yakın mı (aynı yerdeki talepler). */
fun List<LatLng>.allWithin(meters: Double): Boolean {
    val first = firstOrNull() ?: return true
    return all { SphericalUtil.computeDistanceBetween(first, it) < meters }
}
