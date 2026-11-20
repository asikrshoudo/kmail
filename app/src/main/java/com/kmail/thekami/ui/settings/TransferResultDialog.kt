package com.kmail.thekami.ui.settings

import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.kmail.thekami.R
import com.kmail.thekami.data.transfer.TransferText
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Longest text that still gives a QR code a phone camera can read comfortably. */
private const val QR_MAX_CHARS = 900

/** Shows the encrypted transfer as a QR code (when it is small enough), or saves it as a file. */
@Composable
internal fun TransferResultDialog(data: ByteArray, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showQr by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val qrText = remember(data) { TransferText.encode(data) }
    val qrFits = qrText.length <= QR_MAX_CHARS
    val qr = remember(showQr, qrText) { if (showQr && qrFits) buildQr(qrText) else null }
    val saveFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(data) } != null
                    } catch (e: IOException) {
                        false
                    }
                }
                message = context.getString(if (saved) R.string.transfer_saved else R.string.transfer_save_failed)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.transfer_title)) },
        text = {
            SecureWindow()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    qr != null -> {
                        QrImage(qr, Modifier.fillMaxWidth())
                        Text(stringResource(R.string.transfer_qr_note), style = MaterialTheme.typography.bodyMedium)
                    }
                    showQr || !qrFits -> {
                        Text(stringResource(R.string.transfer_qr_too_big), style = MaterialTheme.typography.bodyMedium)
                    }
                    else -> {
                        Button(onClick = { showQr = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.transfer_show_qr))
                        }
                    }
                }
                OutlinedButton(onClick = { saveFile.launch("kmail-accounts.kmail") }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.transfer_save_file))
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.transfer_done)) } },
    )
}

/** Blocks screenshots and screen recording of the dialog it is placed in. */
@Composable
internal fun SecureWindow() {
    val view = LocalView.current
    DisposableEffect(view) {
        (view.parent as? DialogWindowProvider)?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { }
    }
}
