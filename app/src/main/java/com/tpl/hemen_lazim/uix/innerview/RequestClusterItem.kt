package com.tpl.hemen_lazim.uix.innerview

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO

/**
 * Wrapper class to make MaterialRequestDTO compatible with Google Maps clustering
 */
class RequestClusterItem(
    val request: MaterialRequestDTO
) : ClusterItem {
    
    private val position = LatLng(request.latitude, request.longitude)
    
    override fun getPosition(): LatLng = position
    
    override fun getTitle(): String = request.title
    
    override fun getSnippet(): String = request.category.name
    
    override fun getZIndex(): Float = 0f
}

