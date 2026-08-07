package com.grappim.wallosmobile.feature.subscriptions.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.grappim.wallosmobile.core.logger.LogPriority
import com.grappim.wallosmobile.core.logger.logcat
import com.grappim.wallosmobile.feature.subscriptions.domain.model.LogoFile
import java.io.IOException

@Composable
actual fun rememberLogoPickerLauncher(onPick: (LogoFile) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val resolver = context.contentResolver
        try {
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IOException("no input stream for $uri")
            val mimeType = resolver.getType(uri) ?: DEFAULT_MIME_TYPE
            onPick(
                LogoFile(
                    bytes = bytes,
                    fileName = "logo.${mimeType.substringAfterLast('/')}",
                    mimeType = mimeType
                )
            )
        } catch (e: IOException) {
            logcat(priority = LogPriority.WARN, throwable = e) { "Reading the picked logo failed" }
        }
    }
    return { launcher.launch(MIME_FILTER) }
}

private const val DEFAULT_MIME_TYPE = "image/png"
private const val MIME_FILTER = "image/*"
