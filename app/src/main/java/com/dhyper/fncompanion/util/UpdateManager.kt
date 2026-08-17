package com.dhyper.fncompanion.util

import android.content.Context
import com.dhyper.fncompanion.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object UpdateManager {
    private const val GITHUB_API_URL = "https://api.github.com/repos/DHyperYT/FortCompanion/releases/latest"

    suspend fun checkForUpdate(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(GITHUB_API_URL).build()
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to check for updates"))
            
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val json = JSONObject(body)
            val latestTag = json.getString("tag_name").replace("v", "").trim()
            val downloadUrl = json.getString("html_url")
            val currentVersion = BuildConfig.VERSION_NAME.replace("v", "").trim()
            
            val isUpdateAvailable = isVersionNewer(latestTag, currentVersion)
            
            Result.success(UpdateInfo(isUpdateAvailable, latestTag, downloadUrl))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        
        val length = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val l = latestParts.getOrNull(i) ?: 0
            val c = currentParts.getOrNull(i) ?: 0
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersion: String,
        val downloadUrl: String
    )
}
