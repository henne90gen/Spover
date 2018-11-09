package de.spover.spover

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.lang.IllegalArgumentException

class SettingsStore(context: Context) {
    companion object {
        const val FILE_NAME = "SpoverSettingsFile"
        private val TAG = SettingsStore::class.java.simpleName
    }

    private var preferences: SharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(setting: SpoverSettings<T>): T {
        Log.d(TAG, "Retrieved content of ${setting.name}")
        when {
            setting.defaultValue::class == Boolean::class -> {
                return preferences.getBoolean(setting.name, setting.defaultValue as Boolean) as T
            }
            setting.defaultValue::class == Int::class -> {
                return preferences.getInt(setting.name, setting.defaultValue as Int) as T
            }
        }
        throw IllegalArgumentException()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> set(setting: SpoverSettings<T>, value: T) {
        Log.d(TAG, "Updated content of ${setting.name} to $value")
        with(preferences.edit()) {
            when {
                setting.defaultValue::class == Boolean::class -> {
                    putBoolean(setting.name, value as Boolean)
                }
                setting.defaultValue::class == Int::class -> {
                    putInt(setting.name, value as Int)
                }
            }
            apply()
        }
    }
}
