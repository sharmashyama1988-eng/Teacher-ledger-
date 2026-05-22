package com.example.utils

import android.util.Log
import com.example.data.Student
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object GoogleFormSync {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Posts Student detail mapping data asynchronously to a user's defined Google Form response URL.
     */
    suspend fun syncStudentToForm(
        student: Student,
        settings: SettingsManager
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!settings.isGoogleFormSyncEnabled || settings.googleFormUrl.isEmpty()) {
            return@withContext Result.failure(Exception("Google Form sync is not enabled or URL is empty."))
        }

        try {
            val balance = student.totalFee - student.paidAmount
            val formBody = FormBody.Builder()
                .add(settings.formEntryName, student.name)
                .add(settings.formEntryRoll, student.rollNum)
                .add(settings.formEntryPhone, student.phone)
                .add(settings.formEntryTotalFee, student.totalFee.toString())
                .add(settings.formEntryPaid, student.paidAmount.toString())
                .add(settings.formEntryBalance, balance.toString())
                .build()

            val request = Request.Builder()
                .url(settings.googleFormUrl)
                .post(formBody)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Mobile Safari/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Log.d("GoogleFormSync", "Sync failed with code: ${response.code}")
                    Result.failure(Exception("Failed to save data. Code: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleFormSync", "Exception during Google Form sync", e)
            Result.failure(e)
        }
    }
}
