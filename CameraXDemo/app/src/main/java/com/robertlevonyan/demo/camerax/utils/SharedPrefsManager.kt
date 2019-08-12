package com.robertlevonyan.demo.camerax.utils

import android.content.Context

class SharedPrefsManager private constructor(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    companion object {
        private const val PREFERENCES = "sPrefs"

        @Synchronized
        fun newInstance(context: Context) = SharedPrefsManager(context)
    }

    fun putBoolean(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()

    fun putFloat(key: String, value: Float) = preferences.edit().putFloat(key, value).apply()

    fun putInt(key: String, value: Int) = preferences.edit().putInt(key, value).apply()

    fun putLong(key: String, value: Long) = preferences.edit().putLong(key, value).apply()

    fun putString(key: String, value: String) = preferences.edit().putString(key, value).apply()

    fun putStringSet(key: String, value: Set<String>) = preferences.edit().putStringSet(key, value).apply()

    fun getBoolean(key: String, defValue: Boolean) = preferences.getBoolean(key, defValue)

    fun getFloat(key: String, defValue: Float) = preferences.getFloat(key, defValue)

    fun getInt(key: String, defValue: Int) = preferences.getInt(key, defValue)

    fun getLong(key: String, defValue: Long) = preferences.getLong(key, defValue)

    fun getString(key: String, defValue: String) = preferences.getString(key, defValue)

    fun getStringSet(key: String, defValue: Set<String>) = preferences.getStringSet(key, defValue)

    fun getAllValues() = preferences.all

    fun clearData() = preferences.edit().clear().apply()
}