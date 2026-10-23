package com.ssk.chhupanchhupai

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.ssk.chhupanchhupai.data.ApiClient
import com.ssk.chhupanchhupai.data.EventDetail
import com.ssk.chhupanchhupai.data.GameConnection
import com.ssk.chhupanchhupai.data.Loc
import com.ssk.chhupanchhupai.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File

class ChhupanApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    lateinit var prefs: Prefs
        private set
    lateinit var connection: GameConnection
        private set
    lateinit var alerts: Alerts
        private set
    lateinit var api: ApiClient
        private set

    /** The event whose round we're playing (for the share card and "back to event"). */
    val activeEvent = MutableStateFlow<EventDetail?>(null)

    /** Set by a reminder notification: open this event's page. */
    val openEventRequest = MutableStateFlow<String?>(null)

    /** This phone's latest GPS fix (updated by the location service). */
    val myLocation = MutableStateFlow<Loc?>(null)

    @Volatile
    var isInForeground = false
        private set
    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        connection = GameConnection(prefs, appScope) { prefs.identity }
        api = ApiClient(prefs)
        alerts = Alerts(this)

        // OpenStreetMap tiles: identify the app and keep the cache in app storage
        // (no storage permission needed).
        Configuration.getInstance().apply {
            load(this@ChhupanApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = "$packageName/${BuildConfig.VERSION_NAME}"
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }

        appScope.launch {
            connection.notices.collect { alerts.onNotice(it, isInForeground) }
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivities++
                isInForeground = true
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities--
                isInForeground = startedActivities > 0
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
