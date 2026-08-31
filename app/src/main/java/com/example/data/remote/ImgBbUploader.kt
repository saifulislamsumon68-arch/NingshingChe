package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class ImgBbUploadResult(
    val url: String,
    val displayUrl: String,
    val deleteUrl: String,
    val title: String,
    val sizeBytes: Long,
    val mimeType: String
)

object ImgBbUploader {

    private const val MAX_SIZE_BYTES = 32 * 1024 * 1024 // 32 MB max allowed by ImgBB
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun uploadFromUri(
        context: Context,
        uri: Uri,
        customName: String? = null
    ): Result<ImgBbUploadResult> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

            // Validate MIME type
            if (!mimeType.startsWith("image/")) {
                return@withContext Result.failure(IllegalArgumentException("নির্বাচিত ফাইলটি কোনো বৈধ ছবি নয় (Not an image). Type: $mimeType"))
            }

            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("ছবির ফাইল পড়া যাচ্ছে না (Cannot read image file)"))

            val bytes = inputStream.use { it.readBytes() }

            // Validate Size
            if (bytes.size > MAX_SIZE_BYTES) {
                return@withContext Result.failure(
                    IllegalArgumentException(
                        "ছবির আকার ৩২ মেগাবাইটের বেশি হতে পারবে না (Exceeds 32MB limit: ${bytes.size / (1024 * 1024)}MB)"
                    )
                )
            }

            val fileName = customName ?: "img_${System.currentTimeMillis()}"
            uploadBytes(bytes, fileName, mimeType)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadBitmap(
        bitmap: Bitmap,
        customName: String? = null,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 90
    ): Result<ImgBbUploadResult> = withContext(Dispatchers.IO) {
        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(format, quality, stream)
            val bytes = stream.toByteArray()

            if (bytes.size > MAX_SIZE_BYTES) {
                return@withContext Result.failure(
                    IllegalArgumentException("ছবির আকার ৩২ মেগাবাইটের বেশি হতে পারবে না (Image exceeds 32MB)")
                )
            }

            val fileName = customName ?: "img_${System.currentTimeMillis()}"
            val mimeType = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
            uploadBytes(bytes, fileName, mimeType)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadBytes(
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<ImgBbUploadResult> = withContext(Dispatchers.IO) {
        try {
            val apiKey = SupabaseConfig.imgbbApiKey
            if (apiKey.isBlank() || apiKey.startsWith("MY_")) {
                return@withContext Result.failure(IllegalStateException("ImgBB API Key পাওয়া যায়নি। সেটিংস বা এনভায়রনমেন্ট কনফিগারেশন চেক করুন।"))
            }

            val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val formBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", apiKey)
                .addFormDataPart("image", base64Image)
                .addFormDataPart("name", fileName)
                .build()

            val request = Request.Builder()
                .url(SupabaseConfig.imgbbUploadUrl)
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful || responseBody.isBlank()) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                return@withContext Result.failure(Exception("ImgBB আপলোড ব্যর্থ হয়েছে: $errorMsg"))
            }

            val json = JSONObject(responseBody)
            if (!json.optBoolean("success", false)) {
                val err = json.optJSONObject("error")?.optString("message") ?: "অজানা ত্রুটি"
                return@withContext Result.failure(Exception("ImgBB ত্রুটি: $err"))
            }

            val data = json.getJSONObject("data")
            val url = data.optString("url", "")
            val displayUrl = data.optString("display_url", url)
            val deleteUrl = data.optString("delete_url", "")
            val title = data.optString("title", fileName)
            val size = data.optLong("size", bytes.size.toLong())

            Result.success(
                ImgBbUploadResult(
                    url = url,
                    displayUrl = displayUrl,
                    deleteUrl = deleteUrl,
                    title = title,
                    sizeBytes = size,
                    mimeType = mimeType
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun attemptDeleteImage(deleteUrl: String): Boolean = withContext(Dispatchers.IO) {
        if (deleteUrl.isBlank()) return@withContext false
        try {
            val request = Request.Builder()
                .url(deleteUrl)
                .get()
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }
}
