package com.tpl.hemen_lazim.uix.innerview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.tasks.await

@SuppressLint("MissingPermission", "UnrememberedMutableState")
@Composable
fun LocationMapComponent(
    currentLatitude: Double?,
    currentLongitude: Double?,
    onLocationSelected: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(false) }
    var isGpsEnabled by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }

    // Initialize with current coordinates if available
    LaunchedEffect(currentLatitude, currentLongitude) {
        if (currentLatitude != null && currentLongitude != null) {
            currentLocation = LatLng(currentLatitude, currentLongitude)
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (hasLocationPermission && isGpsEnabled) {
            getCurrentLocation(context) { location ->
                currentLocation = location
                onLocationSelected(location.latitude, location.longitude)
            }
        } else if (hasLocationPermission && !isGpsEnabled) {
            showGpsDialog = true
        } else {
            showLocationDialog = true
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
        
        // Auto-fetch location if we have permission and no location is set
        if (hasLocationPermission && isGpsEnabled && currentLocation == null) {
            getCurrentLocation(context) { location ->
                currentLocation = location
                onLocationSelected(location.latitude, location.longitude)
            }
        } else if (!hasLocationPermission && currentLocation == null) {
            // Request permission on first load
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else if (!isGpsEnabled && currentLocation == null) {
            showGpsDialog = true
        }
    }

    Column(modifier = modifier) {
        // Header with current location info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Konum Seçimi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (currentLocation != null) {
                    Text(
                        text = "Enlem: ${String.format("%.6f", currentLocation!!.latitude)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Boylam: ${String.format("%.6f", currentLocation!!.longitude)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "Konum henüz seçilmedi. Haritadan konumunuzu alabilir veya haritaya dokunabilirsiniz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Map - Always visible
        val defaultLocation = currentLocation ?: LatLng(41.0082, 28.9784) // Istanbul center as default
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
        }

        // Update camera when location changes
        LaunchedEffect(currentLocation) {
            currentLocation?.let { location ->
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(location, 14f),
                    durationMs = 500
                )
            }
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
            onMapClick = { latLng ->
                currentLocation = latLng
                onLocationSelected(latLng.latitude, latLng.longitude)
            },
            onMyLocationButtonClick = {
                // When user clicks "My Location" button, update the selected location
                if (hasLocationPermission) {
                    getCurrentLocation(context) { location ->
                        currentLocation = location
                        onLocationSelected(location.latitude, location.longitude)
                    }
                }
                false // Return false to allow default behavior (camera centering)
            }
        ) {
            // Marker for selected location
            currentLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = "Seçilen Konum"
                )
            }
        }
        
        Text(
            text = "Haritada farklı bir konum seçmek için haritaya dokunun. Mavi düğme ile konumunuzu alabilirsiniz.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    // Location Permission Dialog
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Konum İzni Gerekli") },
            text = { Text("Bu özelliği kullanabilmek için konum iznine ihtiyacımız var. Lütfen ayarlardan konum iznini verin.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
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
            text = { Text("Konum almak için GPS'in açık olması gerekiyor. Lütfen GPS'i açın.") },
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
        10000L // 10 seconds
    ).apply {
        setMinUpdateIntervalMillis(5000L) // 5 seconds
        setMaxUpdateDelayMillis(15000L) // 15 seconds
    }.build()

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                onLocationReceived(LatLng(location.latitude, location.longitude))
                fusedLocationClient.removeLocationUpdates(this)
            }
        }
    }

    // Try to get last known location first
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            onLocationReceived(LatLng(location.latitude, location.longitude))
        } else {
            // If no last known location, request new location
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        }
    }.addOnFailureListener {
        // Fallback to default location (Istanbul center)
        onLocationReceived(LatLng(41.0082, 28.9784))
    }
}
