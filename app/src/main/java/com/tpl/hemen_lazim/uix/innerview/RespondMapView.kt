package com.tpl.hemen_lazim.uix.innerview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
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

            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = true,
                    zoomControlsEnabled = true,
                    compassEnabled = true
                ),
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
                }
            ) {
                // Gray circle for search radius
                Circle(
                    center = userLocation,
                    radius = (radiusKm * 1000).toDouble(), // Convert km to meters
                    strokeColor = Color.Gray,
                    strokeWidth = 2f,
                    fillColor = Color.Gray.copy(alpha = 0.1f)
                )

                // Request markers with emojis
                nearbyRequests.forEach { request ->
                    val requestLocation = LatLng(request.latitude, request.longitude)
                    
                    MarkerInfoWindowContent(
                        state = MarkerState(position = requestLocation),
                        title = request.title,
                        snippet = request.category.name,
                        onClick = {
                            onRequestClick(request)
                            true // Consume the click
                        }
                    ) {
                        // Custom emoji marker content
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                text = request.category.toEmoji(),
                                fontSize = 28.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

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
