package com.ssk.chhupanchhupai.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.ssk.chhupanchhupai.Alerts
import com.ssk.chhupanchhupai.ChhupanApp
import com.ssk.chhupanchhupai.R
import com.ssk.chhupanchhupai.data.GameState
import com.ssk.chhupanchhupai.data.Loc
import com.ssk.chhupanchhupai.data.Phase
import com.ssk.chhupanchhupai.data.Role
import com.ssk.chhupanchhupai.data.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps GPS running and the game connection alive while a game is on, even
 * with the screen off in a pocket. Stops itself when the player leaves the game.
 */
class GameLocationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var fused: FusedLocationProviderClient
    private var running = false
    private var lastText: String? = null

    private val app get() = application as ChhupanApp

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(::onLocation)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasLocationPermission(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(describe(app.connection.state.value.game)),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
            )
        } catch (e: Exception) {
            // Android refuses to start location services from the background.
            stopSelf()
            return START_NOT_STICKY
        }
        if (!running) {
            running = true
            startUpdates()
            scope.launch {
                app.connection.state.collect { state ->
                    if (!state.hasSession) {
                        stopSelf()
                        return@collect
                    }
                    updateNotification(describe(state.game))
                }
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            .setWaitForAccurateLocation(false)
            .build()
        fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun onLocation(location: Location) {
        val accuracy = if (location.hasAccuracy()) location.accuracy else null
        app.myLocation.value = Loc(location.latitude, location.longitude, accuracy?.toDouble(), System.currentTimeMillis())
        app.connection.sendLocation(location.latitude, location.longitude, accuracy)
    }

    private fun describe(game: GameState?): String {
        if (game == null) return "Connecting to your game…"
        val role = when {
            game.you.role == Role.SEEKER -> "Seeker"
            game.you.role == Role.HIDER && game.you.status != Status.ACTIVE -> "Spectating"
            game.you.role == Role.HIDER -> "Hider"
            else -> null
        }
        val phase = when (game.phase) {
            Phase.LOBBY -> "In the lobby (code ${game.code})"
            Phase.HEADSTART -> "Head start running"
            Phase.HUNT -> "The hunt is on · ${game.hidersLeft} still hidden"
            else -> "Round over"
        }
        return listOfNotNull(role, phase).joinToString(" · ")
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, Alerts.CHANNEL_GAME)
            .setSmallIcon(R.drawable.ic_stat_eye)
            .setContentTitle("Chhupan Chhupai is sharing your location")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(Alerts.openAppIntent(this))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    private fun updateNotification(text: String) {
        if (text == lastText) return
        lastText = text
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(text))
        } catch (_: SecurityException) {
        }
    }

    override fun onDestroy() {
        fused.removeLocationUpdates(callback)
        scope.cancel()
        running = false
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val UPDATE_INTERVAL_MS = 4000L
        private const val FASTEST_INTERVAL_MS = 2000L

        fun hasLocationPermission(context: Context) =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        /** Must be called while the app is on screen (Android blocks it from the background). */
        fun start(context: Context) {
            if (!hasLocationPermission(context)) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, GameLocationService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
