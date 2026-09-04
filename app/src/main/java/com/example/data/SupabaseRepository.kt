package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class SupabaseRepository(context: Context) {
    private val prefs = context.getSharedPreferences("temp_clipboard_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val inMemoryClips = ConcurrentHashMap<String, ClipItem>()

    init {
        // Pre-populate a demo clip for instant zero-friction testing
        val demoCode = "1234"
        val demoClip = ClipItem(
            code = demoCode,
            content = "Welcome to Temporary Online Clipboard!\n\nThis clip demonstrates cross-device text transfer with instant PIN access and auto-destruction.\nTry creating your own clip or testing Burn on Read.",
            type = "text",
            burnOnRead = false,
            expiresAt = System.currentTimeMillis() + 10 * 60 * 1000L
        )
        inMemoryClips[demoCode] = demoClip
    }

    var supabaseUrl: String
        get() = prefs.getString("supabase_url", "") ?: ""
        set(value) = prefs.edit().putString("supabase_url", value.trim()).apply()

    var supabaseAnonKey: String
        get() = prefs.getString("supabase_anon_key", "") ?: ""
        set(value) = prefs.edit().putString("supabase_anon_key", value.trim()).apply()

    val isConfigured: Boolean
        get() = supabaseUrl.isNotEmpty() && supabaseAnonKey.isNotEmpty()

    private val isoFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    suspend fun createClip(
        content: String?,
        fileBytes: ByteArray? = null,
        fileName: String? = null,
        isImage: Boolean = false,
        burnOnRead: Boolean = false
    ): Result<ClipItem> = withContext(Dispatchers.IO) {
        val code = generateNonCollidingCode()
        val expiresAt = System.currentTimeMillis() + 10 * 60 * 1000L
        val type = when {
            fileBytes != null && isImage -> "image"
            fileBytes != null -> "file"
            else -> "text"
        }

        var filePath: String? = null
        if (fileBytes != null && fileName != null) {
            filePath = "$code/${System.currentTimeMillis()}_$fileName"
        }

        val clip = ClipItem(
            code = code,
            content = content,
            filePath = filePath,
            fileName = fileName,
            fileSize = fileBytes?.size?.toLong(),
            type = type,
            burnOnRead = burnOnRead,
            expiresAt = expiresAt
        )

        // If Supabase is configured, upload to Supabase cloud
        if (isConfigured) {
            try {
                // 1. Upload storage file if any
                if (fileBytes != null && filePath != null) {
                    val storageUrl = "${supabaseUrl.trimEnd('/')}/storage/v1/object/temporary_clips/$filePath"
                    val mediaType = (if (isImage) "image/jpeg" else "application/octet-stream").toMediaType()
                    val uploadReq = Request.Builder()
                        .url(storageUrl)
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $supabaseAnonKey")
                        .post(fileBytes.toRequestBody(mediaType))
                        .build()

                    client.newCall(uploadReq).execute().use { response ->
                        if (!response.isSuccessful) {
                            return@withContext Result.failure(Exception("Storage upload failed: HTTP ${response.code}"))
                        }
                    }
                }

                // 2. Insert row into clipboard table
                val dbUrl = "${supabaseUrl.trimEnd('/')}/rest/v1/clipboard"
                val jsonPayload = JSONObject().apply {
                    put("id", clip.id)
                    put("code", clip.code)
                    put("content", clip.content ?: JSONObject.NULL)
                    put("file_path", clip.filePath ?: JSONObject.NULL)
                    put("file_name", clip.fileName ?: JSONObject.NULL)
                    put("file_size", clip.fileSize ?: JSONObject.NULL)
                    put("type", clip.type)
                    put("burn_on_read", clip.burnOnRead)
                    put("expires_at", isoFormat.format(Date(clip.expiresAt)))
                }

                val dbReq = Request.Builder()
                    .url(dbUrl)
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(dbReq).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("Database insert failed: HTTP ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                // Fallback to local memory on network error with warning
                inMemoryClips[code] = clip
                return@withContext Result.success(clip)
            }
        }

        // Save locally for fallback/speed
        inMemoryClips[code] = clip
        Result.success(clip)
    }

    suspend fun getClip(code: String): Result<ClipItem?> = withContext(Dispatchers.IO) {
        cleanExpiredLocalClips()

        if (isConfigured) {
            try {
                val nowIso = isoFormat.format(Date())
                val url = "${supabaseUrl.trimEnd('/')}/rest/v1/clipboard?code=eq.$code&expires_at=gt.$nowIso&order=created_at.desc&limit=1"

                val req = Request.Builder()
                    .url(url)
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .get()
                    .build()

                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: "[]"
                        val arr = JSONArray(body)
                        if (arr.length() > 0) {
                            val obj = arr.getJSONObject(0)
                            val expiresAtStr = obj.optString("expires_at")
                            val expiresAtDate = try {
                                isoFormat.parse(expiresAtStr)?.time ?: (System.currentTimeMillis() + 10 * 60 * 1000L)
                            } catch (e: Exception) {
                                System.currentTimeMillis() + 10 * 60 * 1000L
                            }

                            val clip = ClipItem(
                                id = obj.optString("id"),
                                code = obj.getString("code"),
                                content = if (obj.isNull("content")) null else obj.getString("content"),
                                filePath = if (obj.isNull("file_path")) null else obj.getString("file_path"),
                                fileName = if (obj.isNull("file_name")) null else obj.getString("file_name"),
                                fileSize = if (obj.isNull("file_size")) null else obj.getLong("file_size"),
                                type = obj.optString("type", "text"),
                                burnOnRead = obj.optBoolean("burn_on_read", false),
                                expiresAt = expiresAtDate
                            )

                            // Burn on Read: delete immediately
                            if (clip.burnOnRead) {
                                deleteClip(clip)
                            }

                            return@withContext Result.success(clip)
                        }
                    }
                }
            } catch (e: Exception) {
                // Fall back to memory check
            }
        }

        // Local cache lookup
        val localClip = inMemoryClips[code]
        if (localClip != null && !localClip.isExpired) {
            if (localClip.burnOnRead) {
                inMemoryClips.remove(code)
            }
            return@withContext Result.success(localClip)
        }

        Result.success(null)
    }

    private suspend fun deleteClip(clip: ClipItem) = withContext(Dispatchers.IO) {
        inMemoryClips.remove(clip.code)
        if (isConfigured) {
            try {
                // Delete row
                val deleteUrl = "${supabaseUrl.trimEnd('/')}/rest/v1/clipboard?id=eq.${clip.id}"
                val deleteReq = Request.Builder()
                    .url(deleteUrl)
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .delete()
                    .build()
                client.newCall(deleteReq).execute().close()

                // Delete file from storage if present
                if (clip.filePath != null) {
                    val storageDeleteUrl = "${supabaseUrl.trimEnd('/')}/storage/v1/object/temporary_clips"
                    val payload = JSONObject().apply {
                        put("prefixes", JSONArray().put(clip.filePath))
                    }
                    val req = Request.Builder()
                        .url(storageDeleteUrl)
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $supabaseAnonKey")
                        .delete(payload.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(req).execute().close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getPublicFileUrl(filePath: String): String {
        return if (isConfigured) {
            "${supabaseUrl.trimEnd('/')}/storage/v1/object/public/temporary_clips/$filePath"
        } else {
            ""
        }
    }

    private fun generateNonCollidingCode(): String {
        var code: String
        do {
            code = (1000 + (Math.random() * 9000).toInt()).toString()
        } while (inMemoryClips.containsKey(code) && !inMemoryClips[code]!!.isExpired)
        return code
    }

    private fun cleanExpiredLocalClips() {
        val now = System.currentTimeMillis()
        val expiredKeys = inMemoryClips.filter { it.value.expiresAt <= now }.keys
        expiredKeys.forEach { inMemoryClips.remove(it) }
    }
}
