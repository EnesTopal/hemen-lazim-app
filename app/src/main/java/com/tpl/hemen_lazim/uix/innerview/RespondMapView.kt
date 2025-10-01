package com.tpl.hemen_lazim.uix.innerview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import com.google.android.gms.maps.GoogleMap
import kotlin.math.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.compose.*
import com.tpl.hemen_lazim.model.DTOs.MaterialRequestDTO
import com.tpl.hemen_lazim.model.enums.toEmoji

@SuppressLint("MissingPermission")
@Composable
fun RespondMapView(
    userLocation: LatLng?,
    radiusKm: Float,
    nearbyRequests: List<MaterialRequestDTO>,
    onLocationReceived: (LatLng) -> Unit,
    onRequestClick: (MaterialRequestDTO) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(false) }
    var isGpsEnabled by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }
    var showMultiRequestDialog by remember { mutableStateOf(false) }
    var requestsAtSameLocation by remember { mutableStateOf<List<MaterialRequestDTO>>(emptyList()) }
    
    // Track last reported location to detect movement
    var lastReportedLocation by remember { mutableStateOf<LatLng?>(null) }

    // Permission launcher for initial request (must be defined before LaunchedEffect)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (hasLocationPermission && isGpsEnabled) {
            isLoadingLocation = true
            getCurrentLocation(context) { location ->
                onLocationReceived(location)
                lastReportedLocation = location // Initialize tracking
                isLoadingLocation = false
            }
        } else if (hasLocationPermission && !isGpsEnabled) {
            showGpsDialog = true
        }
    }

    // Check GPS status and request permission if needed
    LaunchedEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        // Auto-fetch location if we have permission
        if (hasLocationPermission && isGpsEnabled && userLocation == null) {
            isLoadingLocation = true
            getCurrentLocation(context) { location ->
                onLocationReceived(location)
                lastReportedLocation = location // Initialize tracking
                isLoadingLocation = false
            }
        } else if (!hasLocationPermission && userLocation == null) {
            // Request permission on first load
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Column(modifier = modifier) {
        // Map
        if (userLocation != null) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(userLocation, 14f)
            }

            // Update camera when location changes
            LaunchedEffect(userLocation) {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(userLocation, 14f),
                    durationMs = 500
                )
            }

            MapWithClustering(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                cameraPositionState = cameraPositionState,
                hasLocationPermission = hasLocationPermission,
                userLocation = userLocation,
                radiusKm = radiusKm,
                nearbyRequests = nearbyRequests,
                onMyLocationButtonClick = {
                    // When user clicks "My Location" button, track their movement
                    if (hasLocationPermission) {
                        getCurrentLocation(context) { newLocation ->
                            val distance = lastReportedLocation?.let { 
                                calculateDistance(it, newLocation)
                            } ?: Double.MAX_VALUE
                            
                            // If moved more than 10 meters, update search center
                            if (distance > 10.0) {
                                onLocationReceived(newLocation)
                                lastReportedLocation = newLocation
                            }
                        }
                    }
                    false // Return false to allow default behavior (camera centering)
                },
onMarkerClick = { clickedRequests ->
                    // Check if multiple requests at same location
                    if (clickedRequests.size > 1) {
                        requestsAtSameLocation = clickedRequests
                        showMultiRequestDialog = true
                    } else if (clickedRequests.size == 1) {
                        onRequestClick(clickedRequests.first())
                    }
                }
            )

        } else {
            // Placeholder when no location
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isLoadingLocation) {
                            CircularProgressIndicator()
                            Text("Konum alınıyor...")
                        } else {
                            Text(
                                text = "Harita yüklemek için\nkonumunuzu paylaşın",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Location Permission Dialog
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Konum İzni Gerekli") },
            text = { Text("Yakınındaki talepleri görebilmek için konum iznine ihtiyacımız var.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationDialog = false
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        context.startActivity(intent)
                    }
                ) {
                    Text("Ayarlar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // GPS Enable Dialog
    if (showGpsDialog) {
        AlertDialog(
            onDismissRequest = { showGpsDialog = false },
            title = { Text("GPS Kapalı") },
            text = { Text("Konum almak için GPS'in açık olması gerekiyor.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGpsDialog = false
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        context.startActivity(intent)
                    }
                ) {
                    Text("GPS Ayarları")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpsDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
    
    // Multi-Request Dialog (for same location)
    if (showMultiRequestDialog && requestsAtSameLocation.isNotEmpty()) {
        MultiRequestDialog(
            requests = requestsAtSameLocation,
            onRequestSelected = { request ->
                onRequestClick(request)
            },
            onDismiss = {
                showMultiRequestDialog = false
                requestsAtSameLocation = emptyList()
            }
        )
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(
    context: Context,
    onLocationReceived: (LatLng) -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        10000L
    ).apply {
        setMinUpdateIntervalMillis(5000L)
        setMaxUpdateDelayMillis(15000L)
    }.build()

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                onLocationReceived(LatLng(location.latitude, location.longitude))
                fusedLocationClient.removeLocationUpdates(this)
            }
        }
    }

    // Try last known location first
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            onLocationReceived(LatLng(location.latitude, location.longitude))
        } else {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        }
    }.addOnFailureListener {
        // Fallback to Istanbul center
        onLocationReceived(LatLng(41.0082, 28.9784))
    }
}

// Calculate distance between two LatLng points using Haversine formula (returns meters)
private fun calculateDistance(from: LatLng, to: LatLng): Double {
    val earthRadiusMeters = 6371000.0 // Earth's radius in meters
    
    val dLat = Math.toRadians(to.latitude - from.latitude)
    val dLng = Math.toRadians(to.longitude - from.longitude)
    
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) *
            sin(dLng / 2) * sin(dLng / 2)
    
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    
    return earthRadiusMeters * c // Distance in meters
}

@SuppressLint("MissingPermission")
@Composable
private fun MapWithClustering(
    cameraPositionState: CameraPositionState,
    hasLocationPermission: Boolean,
    userLocation: LatLng,
    radiusKm: Float,
    nearbyRequests: List<MaterialRequestDTO>,
    onMyLocationButtonClick: () -> Boolean,
    onMarkerClick: (List<MaterialRequestDTO>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clusterManager = remember { mutableStateOf<ClusterManager<RequestClusterItem>?>(null) }
    
    // Convert requests to cluster items
    val clusterItems = remember(nearbyRequests) {
        nearbyRequests.map { RequestClusterItem(it) }
    }
    
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = true,
            zoomControlsEnabled = true,
            compassEnabled = true
        ),
        onMyLocationButtonClick = onMyLocationButtonClick,
        onMapLoaded = {
            // Map loaded callback
        }
    ) {
        // Draw search radius circle
        Circle(
            center = userLocation,
            radius = (radiusKm * 1000).toDouble(),
            strokeColor = Color.Gray,
            strokeWidth = 2f,
            fillColor = Color.Gray.copy(alpha = 0.1f)
        )
        
        // Set up clustering with MapEffect
        MapEffect(key1 = clusterItems) { map ->
            if (clusterManager.value == null) {
                clusterManager.value = ClusterManager<RequestClusterItem>(context, map).apply {
                    // Custom renderer for emoji markers
                    renderer = RequestClusterRenderer(context, map, this)
                    
                    // Handle cluster item clicks - direct click on marker
                    setOnClusterItemClickListener { item ->
                        // Always include the clicked request, then find any others at same location
                        val clickedRequest = item.request
                        val othersAtSameLocation = nearbyRequests.filter { request ->
                            // Skip the clicked request itself in the filter
                            if (request.id == clickedRequest.id) return@filter false
                            
                            val distance = calculateDistance(
                                LatLng(request.latitude, request.longitude),
                                item.position
                            )
                            distance < 10.0 // Within 10 meters = same location
                        }
                        
                        // Combine clicked request with others at same location
                        val allRequestsAtLocation = listOf(clickedRequest) + othersAtSameLocation
                        
                        onMarkerClick(allRequestsAtLocation)
                        true // Consume the event
                    }
                    
                    // Handle info window clicks (when user taps the popup bubble)
                    setOnClusterItemInfoWindowClickListener { item ->
                        // Always include the clicked request, then find any others at same location
                        val clickedRequest = item.request
                        val othersAtSameLocation = nearbyRequests.filter { request ->
                            // Skip the clicked request itself
                            if (request.id == clickedRequest.id) return@filter false
                            
                            val distance = calculateDistance(
                                LatLng(request.latitude, request.longitude),
                                item.position
                            )
                            distance < 10.0 // Within 10 meters = same location
                        }
                        
                        // Combine clicked request with others at same location
                        val allRequestsAtLocation = listOf(clickedRequest) + othersAtSameLocation
                        
                        onMarkerClick(allRequestsAtLocation)
                    }
                    
                    // Handle cluster clicks - zoom in
                    setOnClusterClickListener { cluster ->
                        val builder = com.google.android.gms.maps.model.LatLngBounds.builder()
                        cluster.items.forEach { builder.include(it.position) }
                        val bounds = builder.build()
                        val padding = 100
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(bounds, padding),
                            500,
                            null
                        )
                        true
                    }
                }
                
                // Set cluster manager as the click listener
                map.setOnMarkerClickListener(clusterManager.value)
                map.setOnInfoWindowClickListener(clusterManager.value)
                map.setOnCameraIdleListener(clusterManager.value)
            }
            
            // Update cluster items
            clusterManager.value?.apply {
                clearItems()
                addItems(clusterItems)
                cluster()
            }
        }
    }
}

/**
 * Custom cluster renderer to show emoji markers and cluster counts
 */
private class RequestClusterRenderer(
    private val context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<RequestClusterItem>
) : DefaultClusterRenderer<RequestClusterItem>(context, map, clusterManager) {
    
    private val emojiCache = mutableMapOf<String, com.google.android.gms.maps.model.BitmapDescriptor>()
    
    override fun onBeforeClusterItemRendered(
        item: RequestClusterItem,
        markerOptions: com.google.android.gms.maps.model.MarkerOptions
    ) {
        // Get emoji for this category
        val emoji = item.request.category.toEmoji()
        
        // Get or create bitmap icon for this emoji
        val icon = emojiCache.getOrPut(emoji) {
            createEmojiMarkerBitmap(emoji)
        }
        
        // Set custom emoji icon
        markerOptions
            .icon(icon)
            .title(item.request.title)
            .snippet("${item.request.category.name} - Detayları görmek için tıklayın")
    }
    
    override fun shouldRenderAsCluster(cluster: com.google.maps.android.clustering.Cluster<RequestClusterItem>): Boolean {
        // Show cluster if 2 or more items and they're NOT at the exact same location
        if (cluster.size < 2) return false
        
        // Check if all items are at the same location (within 1 meter)
        val items = cluster.items.toList()
        val firstPos = items.first().position
        val allSameLocation = items.all { item ->
            calculateDistance(firstPos, item.position) < 1.0
        }
        
        // If all at same location, don't cluster (show as single marker, handled by click)
        return !allSameLocation
    }
    
    /**
     * Create a bitmap marker with emoji icon
     */
    private fun createEmojiMarkerBitmap(emoji: String): com.google.android.gms.maps.model.BitmapDescriptor {
        val size = 120 // Marker size in pixels
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background circle
        val bgPaint = Paint().apply {
            color = 0xFFE3F2FD.toInt() // Light blue background
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius * 0.9f, bgPaint)
        
        // Draw emoji text
        val textPaint = Paint().apply {
            color = 0xFF000000.toInt()
            textSize = size * 0.5f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }
        
        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(emoji, 0, emoji.length, textBounds)
        val textHeight = textBounds.height()
        val textY = radius + textHeight / 2f
        
        canvas.drawText(emoji, radius, textY, textPaint)
        
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
