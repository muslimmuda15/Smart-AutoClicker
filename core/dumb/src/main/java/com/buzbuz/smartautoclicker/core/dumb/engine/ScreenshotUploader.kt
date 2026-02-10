/*
 * Copyright (C) 2024 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.core.dumb.engine

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility class for uploading screenshots to a server.
 */
object ScreenshotUploader {

    private const val TAG = "ScreenshotUploader"
    private const val BOUNDARY = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    private const val LINE_END = "\r\n"
    private const val TWO_HYPHENS = "--"

    /**
     * Convert Bitmap to ByteArray in PNG format.
     *
     * @param bitmap The bitmap to convert
     * @param quality Compression quality (0-100), default 100 for PNG
     * @return ByteArray representation of the bitmap
     */
    fun bitmapToByteArray(bitmap: Bitmap, quality: Int = 100): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, quality, stream)
        return stream.toByteArray()
    }

    /**
     * Upload screenshot to server using multipart/form-data.
     *
     * @param serverUrl The server endpoint URL
     * @param bitmap The screenshot bitmap to upload
     * @param fieldName The form field name (default: "image")
     * @param fileName The file name (default: "screenshot_<timestamp>.png")
     * @param additionalHeaders Optional additional headers (e.g., authentication)
     * @return True if upload successful, false otherwise
     */
    suspend fun uploadScreenshot(
        serverUrl: String,
        bitmap: Bitmap,
        fieldName: String = "image",
        fileName: String = "screenshot_${System.currentTimeMillis()}.png",
        additionalHeaders: Map<String, String> = emptyMap()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val imageData = bitmapToByteArray(bitmap)
            
            val url = URL(serverUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
                setRequestProperty("Connection", "Keep-Alive")
                
                // Add additional headers (e.g., authentication)
                additionalHeaders.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }
            }

            DataOutputStream(connection.outputStream).use { outputStream ->
                // Write image data
                outputStream.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_END)
                outputStream.writeBytes("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$fileName\"$LINE_END")
                outputStream.writeBytes("Content-Type: image/png$LINE_END")
                outputStream.writeBytes(LINE_END)
                outputStream.write(imageData)
                outputStream.writeBytes(LINE_END)
                outputStream.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END)
                outputStream.flush()
            }

            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage

            Log.d(TAG, "Upload response: $responseCode - $responseMessage")

            if (responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Upload successful: $response")
                Result.success(response)
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: responseMessage
                Log.e(TAG, "Upload failed: $responseCode - $errorResponse")
                Result.failure(Exception("Upload failed: $responseCode - $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading screenshot", e)
            Result.failure(e)
        }
    }

    /**
     * Upload screenshot as JSON with base64 encoded image.
     *
     * @param serverUrl The server endpoint URL
     * @param bitmap The screenshot bitmap to upload
     * @param additionalHeaders Optional additional headers
     * @return Result with response or error
     */
    suspend fun uploadScreenshotAsJson(
        serverUrl: String,
        bitmap: Bitmap,
        additionalHeaders: Map<String, String> = emptyMap()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val imageData = bitmapToByteArray(bitmap)
            val base64Image = android.util.Base64.encodeToString(imageData, android.util.Base64.NO_WRAP)
            
            val jsonPayload = """
                {
                    "image": "$base64Image",
                    "timestamp": ${System.currentTimeMillis()},
                    "format": "png"
                }
            """.trimIndent()

            val url = URL(serverUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                
                additionalHeaders.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }
            }

            connection.outputStream.use { outputStream ->
                outputStream.write(jsonPayload.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }

            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage

            Log.d(TAG, "Upload response: $responseCode - $responseMessage")

            if (responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Upload successful: $response")
                Result.success(response)
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: responseMessage
                Log.e(TAG, "Upload failed: $responseCode - $errorResponse")
                Result.failure(Exception("Upload failed: $responseCode - $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading screenshot as JSON", e)
            Result.failure(e)
        }
    }
}
