package com.example.utils

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("teacher_ledger_settings", Context.MODE_PRIVATE)

    var isDarkMode: Boolean
        get() = prefs.getBoolean("dark_mode", false)
        set(value) = prefs.edit().putBoolean("dark_mode", value).apply()

    var isPinLockEnabled: Boolean
        get() = prefs.getBoolean("pin_lock_enabled", false)
        set(value) = prefs.edit().putBoolean("pin_lock_enabled", value).apply()

    var hashedPinCode: String
        get() = prefs.getString("hashed_pin_code", "") ?: ""
        set(value) = prefs.edit().putString("hashed_pin_code", value).apply()

    // --- Google Form Integration Properties ---
    var isGoogleFormSyncEnabled: Boolean
        get() = prefs.getBoolean("google_form_sync_enabled", false)
        set(value) = prefs.edit().putBoolean("google_form_sync_enabled", value).apply()

    var googleFormUrl: String
        get() = prefs.getString("google_form_url", "https://docs.google.com/forms/d/e/1FAIpQLSdfu9_y9R-v65YIcx-gVjB0m5q90y9wE1o_ABCDeFgHiJkLmN/formResponse") ?: ""
        set(value) = prefs.edit().putString("google_form_url", value).apply()

    var formEntryName: String
        get() = prefs.getString("form_entry_name", "entry.1000001") ?: "entry.1000001"
        set(value) = prefs.edit().putString("form_entry_name", value).apply()

    var formEntryRoll: String
        get() = prefs.getString("form_entry_roll", "entry.1000002") ?: "entry.1000002"
        set(value) = prefs.edit().putString("form_entry_roll", value).apply()

    var formEntryPhone: String
        get() = prefs.getString("form_entry_phone", "entry.1000003") ?: "entry.1000003"
        set(value) = prefs.edit().putString("form_entry_phone", value).apply()

    var formEntryTotalFee: String
        get() = prefs.getString("form_entry_total_fee", "entry.1000004") ?: "entry.1000004"
        set(value) = prefs.edit().putString("form_entry_total_fee", value).apply()

    var formEntryPaid: String
        get() = prefs.getString("form_entry_paid", "entry.1000005") ?: "entry.1000005"
        set(value) = prefs.edit().putString("form_entry_paid", value).apply()

    var formEntryBalance: String
        get() = prefs.getString("form_entry_balance", "entry.1000006") ?: "entry.1000006"
        set(value) = prefs.edit().putString("form_entry_balance", value).apply()

    // --- Firebase Sync Session ---
    var firebaseUserEmail: String
        get() = prefs.getString("firebase_user_email", "") ?: ""
        set(value) = prefs.edit().putString("firebase_user_email", value).apply()

    var isFirebaseSyncActive: Boolean
        get() = prefs.getBoolean("firebase_sync_active", false)
        set(value) = prefs.edit().putBoolean("firebase_sync_active", value).apply()

    fun logoutFirebase() {
        prefs.edit().remove("firebase_user_email").putBoolean("firebase_sync_active", false).apply()
    }
}
