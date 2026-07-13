package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Report
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object SupabaseService {
    private const val TAG = "SupabaseService"
    private val client = OkHttpClient()

    val supabaseUrl: String = try {
        BuildConfig.SUPABASE_URL
    } catch (e: Exception) {
        ""
    }

    val supabaseKey: String = try {
        BuildConfig.SUPABASE_ANON_KEY
    } catch (e: Exception) {
        ""
    }

    // Is Supabase properly configured with non-default values?
    val isConfigured: Boolean
        get() = supabaseUrl.isNotEmpty() && 
                supabaseKey.isNotEmpty() && 
                !supabaseUrl.contains("placeholder") && 
                !supabaseUrl.contains("your-project")

    // Simple cache for token/session if logged in via real Supabase
    var sessionToken: String? = null
    var userEmail: String? = null
    var userId: String? = null

    init {
        Log.d(TAG, "Initializing SupabaseService...")
        Log.d(TAG, "Supabase URL: $supabaseUrl")
        Log.d(TAG, "Is Configured: $isConfigured")
    }

    suspend fun login(email: String, password: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            // Simulated login
            return@withContext if (email.isNotEmpty() && password.length >= 6) {
                userEmail = email
                Result.success(true)
            } else {
                Result.failure(Exception("Email tidak boleh kosong dan password minimal 6 karakter"))
            }
        }

        val url = "$supabaseUrl/auth/v1/token?grant_type=password"
        val json = JSONObject().apply {
            put("email", email)
            put("password", password)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseKey)
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val jsonObj = JSONObject(bodyStr)
                sessionToken = jsonObj.optString("access_token")
                val userObj = jsonObj.optJSONObject("user")
                userEmail = userObj?.optString("email") ?: email
                userId = userObj?.optString("id")
                Result.success(true)
            } else {
                val errorMsg = try {
                    JSONObject(bodyStr).optString("error_description", "Login gagal")
                } catch (e: Exception) {
                    "Login gagal (HTTP ${response.code})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during login", e)
            Result.failure(Exception("Koneksi internet bermasalah: ${e.localizedMessage}"))
        }
    }

    suspend fun logout(): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            sessionToken = null
            userEmail = null
            userId = null
            return@withContext Result.success(true)
        }

        val url = "$supabaseUrl/auth/v1/logout"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer ${sessionToken ?: supabaseKey}")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute()
            sessionToken = null
            userEmail = null
            userId = null
            Result.success(true)
        } catch (e: Exception) {
            sessionToken = null
            userEmail = null
            userId = null
            Result.success(true)
        }
    }

    suspend fun fetchReports(): Result<List<Report>> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            // Simulated fetch - return empty or let Repository use Room cache
            return@withContext Result.success(emptyList())
        }

        val url = "$supabaseUrl/rest/v1/reports?select=*"
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer ${sessionToken ?: supabaseKey}")
            .get()

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            val bodyStr = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val jsonArray = JSONArray(bodyStr)
                val reports = mutableListOf<Report>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    reports.add(
                        Report(
                            id = obj.optLong("id", 0),
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            status = obj.optString("status", "DILAPORKAN"),
                            createdAt = System.currentTimeMillis(), // Fallback to current local time
                            imageUrl = if (obj.isNull("image_url")) null else obj.optString("image_url", null)
                        )
                    )
                }
                Result.success(reports)
            } else {
                Result.failure(Exception("Gagal mengambil data dari Supabase (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching reports from Supabase", e)
            Result.failure(e)
        }
    }

    suspend fun fetchCurrentUser(): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.success("simulated_user_id")
        }
        val token = sessionToken
        if (token.isNullOrEmpty()) {
            return@withContext Result.failure(Exception("Sesi login kedaluwarsa atau tidak valid"))
        }

        val url = "$supabaseUrl/auth/v1/user"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val jsonObj = JSONObject(bodyStr)
                val id = jsonObj.optString("id")
                if (id.isNotEmpty()) {
                    userId = id
                    Result.success(id)
                } else {
                    Result.failure(Exception("User ID tidak ditemukan dalam response Supabase"))
                }
            } else {
                Result.failure(Exception("Gagal mengambil profil user dari Supabase (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user from Supabase", e)
            Result.failure(e)
        }
    }

    suspend fun insertReport(title: String, description: String, imageUrl: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.success(true)
        }

        if (userId.isNullOrEmpty() && !sessionToken.isNullOrEmpty()) {
            val fetchResult = fetchCurrentUser()
            if (fetchResult.isSuccess) {
                userId = fetchResult.getOrNull()
            }
        }

        if (userId.isNullOrEmpty()) {
            return@withContext Result.failure(Exception("Gagal membuat laporan: Sesi pengguna tidak ditemukan. Silakan login ulang."))
        }

        val url = "$supabaseUrl/rest/v1/reports"
        val json = JSONObject().apply {
            put("title", title)
            put("description", description)
            put("status", "DILAPORKAN")
            put("user_id", userId)
            imageUrl?.let { put("image_url", it) }
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer ${sessionToken ?: supabaseKey}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val bodyStr = response.body?.string() ?: ""
                val errorMsg = try {
                    JSONObject(bodyStr).optString("message", "Gagal menyimpan")
                } catch (e: Exception) {
                    "Gagal menyimpan ke Supabase (HTTP ${response.code})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting report to Supabase", e)
            Result.failure(e)
        }
    }

    suspend fun deleteReport(id: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.success(true)
        }

        val url = "$supabaseUrl/rest/v1/reports?id=eq.$id"
        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer ${sessionToken ?: supabaseKey}")
            .delete()
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val bodyStr = response.body?.string() ?: ""
                val errorMsg = try {
                    JSONObject(bodyStr).optString("message", "Gagal menghapus")
                } catch (e: Exception) {
                    "Gagal menghapus dari Supabase (HTTP ${response.code})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting report from Supabase", e)
            Result.failure(e)
        }
    }
}
