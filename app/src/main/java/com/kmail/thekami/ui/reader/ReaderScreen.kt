package com.kmail.thekami.ui.reader

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.text.format.Formatter
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmail.thekami.R
import com.kmail.thekami.mail.Mailbox
import com.kmail.thekami.mail.ParsedAttachment
import com.kmail.thekami.mail.ParsedMessage
import com.kmail.thekami.ui.components.ErrorState
import com.kmail.thekami.ui.components.LoadingState
import com.kmail.thekami.ui.setup.messageRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onCompose: (String, Long) -> Unit,
    modifier: Modifier = Modifier,
    vm: ReaderViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var pendingLink by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.reader_back))
                    }
                },
                actions = {
                    if (state.message != null) {
                        TextButton(onClick = { onCompose("reply", vm.messageId) }) { Text(stringResource(R.string.compose_reply)) }
                        TextButton(onClick = { onCompose("forward", vm.messageId) }) { Text(stringResource(R.string.compose_forward)) }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.reader_more))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.compose_reply_all)) },
                                    onClick = {
                                        menuOpen = false
                                        onCompose("replyall", vm.messageId)
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(
                                                if (state.allowRemote) R.string.reader_hide_images
                                                else R.string.reader_show_images,
                                            ),
                                        )
                                    },
                                    onClick = {
                                        menuOpen = false
                                        vm.setAllowRemote(!state.allowRemote)
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val s = state
            val message = s.message
            val error = s.error
            when {
                s.loading -> LoadingState(message = stringResource(R.string.loading_generic))
                s.missing -> Text(
                    stringResource(R.string.reader_missing),
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
                error != null -> ErrorState(
                    title = stringResource(R.string.reader_error_title),
                    message = stringResource(error.messageRes()),
                    retryLabel = stringResource(R.string.connecting_retry),
                    onRetry = vm::load,
                )
                message != null -> ReaderContent(
                    message = message,
                    html = s.html,
                    blockedRemote = s.blockedRemote,
                    allowRemote = s.allowRemote,
                    onShowImages = { vm.setAllowRemote(true) },
                    onLink = { pendingLink = it },
                )
            }
        }
    }

    pendingLink?.let { url ->
        AlertDialog(
            onDismissRequest = { pendingLink = null },
            title = { Text(stringResource(R.string.reader_link_title)) },
            text = { Text(url, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingLink = null
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE),
                            )
                        } catch (e: ActivityNotFoundException) {
                            // no app can open this link
                        }
                    },
                ) { Text(stringResource(R.string.reader_link_open)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingLink = null }) { Text(stringResource(R.string.reader_link_cancel)) }
            },
        )
    }
}

@Composable
private fun ReaderContent(
    message: ParsedMessage,
    html: String,
    blockedRemote: Int,
    allowRemote: Boolean,
    onShowImages: () -> Unit,
    onLink: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<ParsedAttachment?>(null) }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val attachment = pending
        pending = null
        if (uri != null && attachment != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(attachment.bytes) }
                            ?: error("no stream")
                    }.isSuccess
                }
                Toast.makeText(
                    context,
                    if (ok) R.string.reader_saved else R.string.reader_save_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ReaderHeader(
            message = message,
            showImagesBanner = blockedRemote > 0 && !allowRemote,
            onShowImages = onShowImages,
            onSave = {
                pending = it
                saveLauncher.launch(it.fileName)
            },
        )
        HorizontalDivider()
        EmailWebView(html = html, allowRemote = allowRemote, onLink = onLink, modifier = Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun ReaderHeader(
    message: ParsedMessage,
    showImagesBanner: Boolean,
    onShowImages: () -> Unit,
    onSave: (ParsedAttachment) -> Unit,
) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val from = message.from
    val senderName = from?.name ?: from?.address ?: stringResource(R.string.home_unknown_sender)
    val initial = senderName.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"
    val date = message.dateMillis?.let {
        DateUtils.formatDateTime(
            context, it,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR,
        )
    }

    Column(
        Modifier
            .heightIn(max = 320.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message.subject.ifBlank { stringResource(R.string.home_no_subject) },
            style = MaterialTheme.typography.headlineSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(initial, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(senderName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (from != null) {
                    Text(from.address, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
                Text(recipientSummary(message.to), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(if (expanded) R.string.reader_details_hide else R.string.reader_details_show))
            }
        }
        if (expanded) {
            DetailLine(stringResource(R.string.reader_label_from), from?.display())
            DetailLine(stringResource(R.string.reader_label_to), message.to.joinToString(", ") { it.display() }.ifEmpty { null })
            DetailLine(stringResource(R.string.reader_label_cc), message.cc.joinToString(", ") { it.display() }.ifEmpty { null })
            DetailLine(stringResource(R.string.reader_label_date), date)
            DetailLine(stringResource(R.string.reader_label_id), message.messageId)
        } else if (date != null) {
            Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showImagesBanner) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.reader_images_blocked),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onShowImages) { Text(stringResource(R.string.reader_show_images)) }
                }
            }
        }
        if (message.attachments.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                message.attachments.forEach { attachment ->
                    AssistChip(
                        onClick = { onSave(attachment) },
                        label = {
                            Text("${attachment.fileName} \u00B7 " + Formatter.formatShortFileSize(context, attachment.sizeBytes))
                        },
                    )
                }
            }
        }
    }
}
@Composable
private fun recipientSummary(to: List<Mailbox>): String = when {
    to.isEmpty() -> stringResource(R.string.reader_no_recipients)
    to.size == 1 -> stringResource(R.string.reader_to_summary, to[0].name ?: to[0].address)
    else -> stringResource(R.string.reader_to_more, to[0].name ?: to[0].address, to.size - 1)
}

@Composable
private fun DetailLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(72.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun EmailWebView(
    html: String,
    allowRemote: Boolean,
    onLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnLink by rememberUpdatedState(onLink)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.domStorageEnabled = false
                settings.safeBrowsingEnabled = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                // Emails are designed for a white page, whatever the app theme is.
                setBackgroundColor(android.graphics.Color.WHITE)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val url = request.url
                        if (url.scheme?.lowercase() in setOf("http", "https", "mailto", "tel")) {
                            currentOnLink(url.toString())
                        }
                        return true
                    }
                }
            }
        },
        update = { view ->
            view.settings.blockNetworkLoads = !allowRemote
            if (view.tag != html) {
                view.tag = html
                view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
        onRelease = { it.destroy() },
    )
}
