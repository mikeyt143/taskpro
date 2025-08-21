package org.tasks.analytics

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.jobs.WorkManager
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Firebase @Inject constructor(
        @ApplicationContext private val context: Context,
        private val preferences: Preferences
) {
    private val crashlytics by lazy {
        if (preferences.isTrackingEnabled) {
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(true)
            }
        } else {
            null
        }
    }
    
    private val analytics by lazy {
        if (preferences.isTrackingEnabled) {
            FirebaseAnalytics.getInstance(context).apply {
                setAnalyticsCollectionEnabled(true)
            }
        } else {
            null
        }
    }
    
    private val remoteConfig by lazy {
        if (preferences.isTrackingEnabled) {
            FirebaseRemoteConfig.getInstance().apply {
                setConfigSettingsAsync(remoteConfigSettings {
                    minimumFetchIntervalInSeconds =
                            TimeUnit.HOURS.toSeconds(WorkManager.REMOTE_CONFIG_INTERVAL_HOURS)
                })
                setDefaultsAsync(R.xml.remote_config_defaults)
            }
        } else {
            null
        }
    }

    fun reportException(t: Throwable) {
        Timber.e(t)
        crashlytics?.recordException(t)
    }

    fun reportIabResult(result: String, sku: String, state: String) {
        logEvent(
            R.string.event_purchase_result,
            R.string.param_sku to sku,
            R.string.param_result to result,
            R.string.param_state to state,
        )
    }

    fun updateRemoteConfig() {
        remoteConfig?.fetchAndActivate()?.addOnSuccessListener {
            Timber.d(it.toString())
        }
    }

    fun addTask(source: String) =
        logEvent(R.string.event_add_task, R.string.param_type to source)

    fun completeTask(source: String) =
        logEvent(R.string.event_complete_task, R.string.param_type to source)

    fun logEvent(@StringRes event: Int, vararg p: Pair<Int, Any>) {
        val eventName = context.getString(event)
        Timber.d("$eventName -> $p")
        analytics?.logEvent(eventName, Bundle().apply {
            p.forEach {
                val key = context.getString(it.first)
                when (it.second::class) {
                    String::class -> putString(key, it.second as String)
                    Boolean::class -> putBoolean(key, it.second as Boolean)
                    else -> Timber.e("Unhandled param: $it")
                }
            }
        })
    }

    private val installCooldown: Boolean
        get() = preferences.installDate + days("install_cooldown", 14L) > currentTimeMillis()

    val reviewCooldown: Boolean
        get() = installCooldown || preferences.lastReviewRequest + days("review_cooldown", 30L) > currentTimeMillis()

    val subscribeCooldown: Boolean
        get() = installCooldown
                || preferences.lastSubscribeRequest + days("subscribe_cooldown", 30L) > currentTimeMillis()

    private fun days(key: String, default: Long): Long =
            TimeUnit.DAYS.toMillis(remoteConfig?.getLong(key) ?: default)
}