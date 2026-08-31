package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InstalledApkInfo(
    val fileName: String,
    val sizeFormatted: String,
    val sizeBytes: Long,
    val versionName: String,
    val versionCode: Long,
    val sourcePath: String,
    val lastModifiedFormatted: String
)

object ApkManager {

    fun getInstalledApkInfo(context: Context): InstalledApkInfo {
        return try {
            val appInfo = context.applicationInfo
            val sourceApkFile = File(appInfo.sourceDir)
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            val versionName = packageInfo.versionName ?: "1.0"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            val sizeBytes = if (sourceApkFile.exists()) sourceApkFile.length() else 0L
            val sizeMb = sizeBytes / (1024.0 * 1024.0)
            val sizeFormatted = String.format(Locale.US, "%.1f MB", sizeMb)

            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val lastModified = if (sourceApkFile.exists()) {
                dateFormat.format(Date(sourceApkFile.lastModified()))
            } else {
                dateFormat.format(Date())
            }

            InstalledApkInfo(
                fileName = "Ningshingche_v$versionName.apk",
                sizeFormatted = sizeFormatted,
                sizeBytes = sizeBytes,
                versionName = versionName,
                versionCode = versionCode,
                sourcePath = appInfo.sourceDir ?: "",
                lastModifiedFormatted = lastModified
            )
        } catch (e: Exception) {
            InstalledApkInfo(
                fileName = "Ningshingche_v1.0.apk",
                sizeFormatted = "Ready",
                sizeBytes = 0L,
                versionName = "1.0",
                versionCode = 1L,
                sourcePath = "",
                lastModifiedFormatted = ""
            )
        }
    }

    suspend fun saveApkToDownloads(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val appInfo = context.applicationInfo
            val sourceFile = File(appInfo.sourceDir)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("Installed APK file not found at source path"))
            }

            val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
            val targetFileName = "Ningshingche_v$versionName.apk"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, targetFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Ningshingche")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri).use { outStream ->
                        if (outStream == null) throw Exception("Failed to open output stream")
                        FileInputStream(sourceFile).use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)

                    return@withContext Result.success("Downloads/Ningshingche/$targetFileName ফোল্ডারে সফলভাবে সংরক্ষিত হয়েছে!")
                }
            }

            // Fallback for older versions or if MediaStore insertion fails
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destDir = File(downloadsDir, "Ningshingche").apply { mkdirs() }
            val destFile = File(destDir, targetFileName)

            FileInputStream(sourceFile).use { inStream ->
                FileOutputStream(destFile).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }

            Result.success("ডাউনলোড ফোল্ডারে সংরক্ষিত হয়েছে: ${destFile.absolutePath}")
        } catch (e: Exception) {
            // Internal app external files fallback
            try {
                val appInfo = context.applicationInfo
                val sourceFile = File(appInfo.sourceDir)
                val extDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val destFile = File(extDownloads, "Ningshingche_v1.0.apk")
                FileInputStream(sourceFile).use { inStream ->
                    FileOutputStream(destFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                Result.success("ডিভাইসে সফলভাবে ডাউনলোড হয়েছে: ${destFile.name}")
            } catch (fallbackEx: Exception) {
                Result.failure(e)
            }
        }
    }

    fun shareInstalledApk(context: Context): Boolean {
        return try {
            val appInfo = context.applicationInfo
            val sourceFile = File(appInfo.sourceDir)
            if (!sourceFile.exists()) return false

            val versionName = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
            } catch (e: Exception) {
                "1.0"
            }

            // Copy to cache dir for FileProvider
            val cacheExportDir = File(context.cacheDir, "apk_exports").apply { mkdirs() }
            val exportFile = File(cacheExportDir, "Ningshingche_v$versionName.apk")

            FileInputStream(sourceFile).use { inStream ->
                FileOutputStream(exportFile).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                exportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                putExtra(Intent.EXTRA_SUBJECT, "Ningshingche Bengali Encyclopedia APK (v$versionName)")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "নিংশিং চে — বিষ্ণুপ্রিয়া মণিপুরি ডিজিটাল তথ্যকোষ ও আর্কাইভ অ্যান্ড্রয়েড অ্যাপ (সংস্করণ $versionName)। ইনস্টল করতে সরাসরি এই APK ফাইলটি ব্যবহার করুন।"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "নিংশিং চে APK ফাইল পাঠান / শেয়ার করুন")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
