package com.dpi

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import com.auriqo.music.constants.DensityScaleKey
import com.auriqo.music.utils.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber

internal const val DEFAULT_DENSITY_SCALE_FACTOR = 1.0f

/** Returns the canonical persisted density value used before the app UI is created. */
internal fun startupDensityScale(preferences: Preferences): Float =
    preferences[DensityScaleKey] ?: DEFAULT_DENSITY_SCALE_FACTOR

class DensityScaler : BaseLifecycleContentProvider() {

    override fun onCreate(): Boolean {
        val context = context ?: return false
        val scaleFactor = getScaleFactorFromPreferences(context)
        DensityConfiguration(scaleFactor).applyDensityScaling(context)
        return true
    }

    companion object {
        private fun getScaleFactorFromPreferences(context: Context): Float {
            return try {
                runBlocking(Dispatchers.IO) {
                    startupDensityScale(context.dataStore.data.first())
                }
            } catch (e: Exception) {
                Timber.tag("DensityScaler").w(e, "Failed to read scale factor from DataStore")
                DEFAULT_DENSITY_SCALE_FACTOR
            }
        }
    }
}
