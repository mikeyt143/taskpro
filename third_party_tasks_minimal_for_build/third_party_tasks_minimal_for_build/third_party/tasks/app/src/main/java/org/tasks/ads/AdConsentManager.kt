package org.tasks.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import timber.log.Timber
import com.google.android.gms.ads.MobileAds

/**
 * Lightweight wrapper for UMP consent and AdMob initialization.
 * This file intentionally keeps logic minimal — integrate more robust flows as needed.
 */
object AdConsentManager {

    /** Initialize UMP and (optionally) the Mobile Ads SDK. Call from Application.onCreate(). */
    fun initialize(application: Application) {
        try {
            // UMP: request consent info
            val params = ConsentRequestParameters.Builder().build()
            val consentInformation = UserMessagingPlatform.getConsentInformation(application)
            consentInformation.requestConsentInfoUpdate(
                application,
                params,
                { /* success */ },
                { formError -> Timber.w("UMP consent info update failed: %s", formError) }
            )
            // Note: Do not automatically show the consent form here. Show from an Activity when needed.
            // Mobile Ads SDK initialization can be delayed until you have consent or choose to initialize immediately.
            // MobileAds initialization: initialize after consent or immediately for non-personalized ads.
            try {
                MobileAds.initialize(application) { initializationStatus ->
                    Timber.d("MobileAds initialized: %s", initializationStatus.toString())
                }
            } catch (e: Exception) {
                Timber.w(e, "MobileAds init failed")
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to initialize ad consent manager")
        }
    }

    /** Simple helper to show the consent form from an Activity later. */
    fun showConsentForm(activity: Activity, onComplete: () -> Unit = {}) {
        try {
            UserMessagingPlatform.loadConsentForm(activity, { consentForm ->
                if (consentForm != null) {
                    consentForm.show(activity) { formError ->
                        if (formError == null) {
                            onComplete()
                        } else {
                            Timber.w(formError, "Consent form show error")
                            onComplete()
                        }
                    }
                } else {
                    onComplete()
                }
            }, { loadError ->
                Timber.w(loadError, "Consent form load error")
                onComplete()
            })
        } catch (e: Exception) {
            Timber.w(e, "Failed to show consent form")
            onComplete()
        }
    }
}
