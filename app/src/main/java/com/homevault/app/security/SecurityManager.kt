package com.homevault.app.security

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object SecurityManager {
    private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB DoS limit

    suspend fun copySharedInvoiceToSandbox(context: Context, externalUri: Uri): File =
        withContext(Dispatchers.IO) {
            require(externalUri.scheme == ContentResolver.SCHEME_CONTENT) {
                "Security violation: Only content:// URIs are permitted."
            }

            val resolver = context.contentResolver
            val mimeType = resolver.getType(externalUri) ?: ""
            require(mimeType.startsWith("image/")) {
                "Unsupported file type: $mimeType"
            }

            val sandboxDir = File(context.filesDir, "secure_invoices").apply {
                if (!exists()) mkdirs()
            }
            val safeFile = File(sandboxDir, "inv_${UUID.randomUUID()}.jpg")

            resolver.openInputStream(externalUri)?.use { input ->
                FileOutputStream(safeFile).use { output ->
                    val buffer = ByteArray(8192)
                    var totalBytes = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        totalBytes += read
                        if (totalBytes > MAX_FILE_SIZE_BYTES) {
                            safeFile.delete()
                            throw SecurityException("Invoice file exceeds 10 MB security limit.")
                        }
                        output.write(buffer, 0, read)
                    }
                }
            } ?: throw IllegalArgumentException("Unable to open shared stream.")

            safeFile
        }
}