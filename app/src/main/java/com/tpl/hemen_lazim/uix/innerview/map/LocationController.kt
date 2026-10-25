package com.tpl.hemen_lazim.uix.innerview.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)
private const val MAX_LOCATION_AGE_MS = 2 * 60_000L
private const val LOCATION_TIMEOUT_MS = 15_000L

sealed interface LocationStatus {
    data object Loading : LocationStatus
    data object Ready : LocationStatus
    /** [canAskAgain] false ise sistem diyaloğu bir daha çıkmaz; yalnızca ayarlardan verilebilir. */
    data class PermissionDenied(val canAskAgain: Boolean) : LocationStatus
    data object LocationOff : LocationStatus
    data object Unavailable : LocationStatus
}

/**
 * İki harita ekranının ortak konum mantığı: izin, konum servisinin açık olması ve güncel
 * konumun alınması. Eskiden her ekranda ayrı bir kopyası vardı; biri izin reddini hiç
 * göstermiyor, ikisi de konum alınamayınca sessizce İstanbul'u kullanıyordu.
 */
class LocationController internal constructor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    var status: LocationStatus by mutableStateOf(LocationStatus.Loading)
        private set

    /** Son alınan konum; yenilenirken de korunur ki harita kaybolmasın. */
    var lastLocation: LatLng? by mutableStateOf(null)
        private set

    /** Her başarılı konum alımında artar: konum aynı kalsa da "Konumuma git" tepki versin. */
    var locationVersion by mutableIntStateOf(0)
        private set

    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var settingsLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var job: Job? = null
    // İzin isteği başka bir izin diyaloğu açıkken (ör. açılıştaki bildirim izni) kesildiyse
    private var retryPermissionOnResume = false

    internal fun bind(
        permissions: ActivityResultLauncher<Array<String>>,
        settings: ActivityResultLauncher<IntentSenderRequest>
    ) {
        permissionLauncher = permissions
        settingsLauncher = settings
    }

    /** Konumu (yeniden) al; izin yoksa ister, konum kapalıysa açtırma diyaloğunu gösterir. */
    fun refresh() {
        if (!hasLocationPermission(context)) {
            // Başlatıcı henüz bağlanmadıysa "Yükleniyor"da takılı kalmasın: butonla tekrar denenir
            permissionLauncher?.launch(LOCATION_PERMISSIONS)
                ?: run { status = LocationStatus.PermissionDenied(canAskAgain = true) }
            return
        }
        job?.cancel()
        job = scope.launch {
            status = LocationStatus.Loading
            if (ensureLocationEnabled()) fetchLocation()
        }
    }

    /** Konum durumu ekranındaki buton: kalıcı reddedilmiş izin ayarlardan, diğerleri yeniden deneyerek. */
    fun resolve() {
        val current = status
        if (current is LocationStatus.PermissionDenied && !current.canAskAgain) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            )
        } else {
            refresh()
        }
    }

    internal fun onPermissionResult(grants: Map<String, Boolean>, canAskAgain: Boolean) {
        when {
            grants.values.any { it } -> refresh()
            // Boş sonuç: istek gösterilmeden iptal edildi (aynı anda tek izin diyaloğu açılabiliyor).
            // Kullanıcı reddetmiş sayılmaz; ekran öne gelince bir kez daha sorulur.
            grants.isEmpty() -> {
                status = LocationStatus.PermissionDenied(canAskAgain = true)
                retryPermissionOnResume = true
            }
            else -> status = LocationStatus.PermissionDenied(canAskAgain)
        }
    }

    internal fun onSettingsResult(enabled: Boolean) {
        if (!enabled) {
            status = LocationStatus.LocationOff
            return
        }
        job?.cancel()
        job = scope.launch {
            status = LocationStatus.Loading
            fetchLocation()
        }
    }

    /**
     * Ayarlardan dönüldüğünde izin ya da konum orada açılmış olabilir. Konum kapalıyken yalnızca
     * gerçekten açıldıysa denenir: aksi halde açtırma diyaloğu kapanınca yine açılır, döngüye girerdi.
     */
    internal fun onResume() {
        if (retryPermissionOnResume) {
            retryPermissionOnResume = false
            refresh()
            return
        }
        val retry = when (status) {
            is LocationStatus.PermissionDenied -> hasLocationPermission(context)
            LocationStatus.LocationOff -> hasLocationPermission(context) && isLocationEnabled()
            else -> false
        }
        if (retry) refresh()
    }

    private suspend fun ensureLocationEnabled(): Boolean {
        val request = LocationSettingsRequest.Builder()
            .addLocationRequest(LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L).build())
            .build()
        return try {
            LocationServices.getSettingsClient(context).checkLocationSettings(request).await()
            true
        } catch (e: ResolvableApiException) {
            status = LocationStatus.LocationOff
            settingsLauncher?.launch(IntentSenderRequest.Builder(e.resolution).build())
            false
        } catch (e: ApiException) {
            // Ayar denetimi başka bir nedenle yapılamadı: konum açıksa yine de denenir
            isLocationEnabled().also { enabled -> if (!enabled) status = LocationStatus.LocationOff }
        }
    }

    @SuppressLint("MissingPermission") // refresh() izni denetliyor
    private suspend fun fetchLocation() {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setMaxUpdateAgeMillis(MAX_LOCATION_AGE_MS)
            .setDurationMillis(LOCATION_TIMEOUT_MS)
            .build()
        val location = try {
            LocationServices.getFusedLocationProviderClient(context).getCurrentLocation(request, null).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (location == null) {
            status = LocationStatus.Unavailable
            return
        }
        lastLocation = LatLng(location.latitude, location.longitude)
        locationVersion++
        status = LocationStatus.Ready
    }

    private fun isLocationEnabled(): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(manager)
    }
}

/** Ekrana girildiğinde konumu bir kez ister; ayarlardan dönünce gerekirse yeniden dener. */
@Composable
fun rememberLocationController(): LocationController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember(context) { LocationController(context, scope) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        // Reddedildikten sonra gerekçe gösterilemiyorsa kullanıcı "bir daha sorma" demiştir
        val activity = context.findActivity()
        val canAskAgain = activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        controller.onPermissionResult(grants, canAskAgain)
    }
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        controller.onSettingsResult(enabled = result.resultCode == Activity.RESULT_OK)
    }
    SideEffect { controller.bind(permissionLauncher, settingsLauncher) }

    // Önizlemede konum istenmez: izin diyaloğu açılamıyor (harita kartı yerine çizim koyuyor)
    val inPreview = LocalInspectionMode.current
    LaunchedEffect(controller) { if (!inPreview) controller.refresh() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) controller.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return controller
}

/** Haritada mavi nokta gösterilebilir mi: izin yokken açmak SecurityException fırlatır. */
fun hasLocationPermission(context: Context): Boolean =
    LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
