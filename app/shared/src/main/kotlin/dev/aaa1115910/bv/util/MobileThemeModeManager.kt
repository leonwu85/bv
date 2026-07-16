package dev.aaa1115910.bv.util

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.intPreferencesKey
import de.schnettler.datastore.manager.PreferenceRequest
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.entity.ThemeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

object MobileThemeModeManager {
    private val themeTypeKey = intPreferencesKey("mobile_theme_type")
    private val themeTypeRequest = PreferenceRequest(themeTypeKey, ThemeType.Auto.ordinal)

    val themeType: ThemeType
        get() = runBlocking {
            resolveThemeType(BVApp.dataStoreManager.getPreferenceFlow(themeTypeRequest).first())
        }

    val themeTypeFlow: Flow<ThemeType>
        get() = BVApp.dataStoreManager.getPreferenceFlow(themeTypeRequest)
            .map(::resolveThemeType)

    fun setThemeType(value: ThemeType, context: Context = BVApp.context) {
        runBlocking {
            BVApp.dataStoreManager.editPreference(themeTypeKey, value.ordinal)
        }
        applyThemeType(context, value)
    }

    fun sync(context: Context = BVApp.context) {
        applyThemeType(context, themeType)
    }

    private fun applyThemeType(context: Context, themeType: ThemeType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mode = when (themeType) {
                ThemeType.Auto -> UiModeManager.MODE_NIGHT_AUTO
                ThemeType.Dark -> UiModeManager.MODE_NIGHT_YES
                ThemeType.Light -> UiModeManager.MODE_NIGHT_NO
            }
            context.getSystemService(UiModeManager::class.java).setApplicationNightMode(mode)
        } else {
            val mode = when (themeType) {
                ThemeType.Auto -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeType.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeType.Light -> AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    private fun resolveThemeType(ordinal: Int): ThemeType =
        ThemeType.entries.getOrElse(ordinal) { ThemeType.Auto }
}
