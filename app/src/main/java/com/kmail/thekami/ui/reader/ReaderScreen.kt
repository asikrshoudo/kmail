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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.ReplyAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmail.thekami.R
import com.kmail.thekami.mail.Mailbox
import com.kmail.thekami.mail.ParsedAttachment
import com.kmail.thekami.mail.ParsedMessage
import com.kmail.thekami.ui.components.ErrorState
import com.kmail.thekami.ui.home.AvatarColors
import com.kmail.thekami.ui.home.actionNoticeText
import com.kmail.thekami.ui.home.folderIcon
import com.kmail.thekami.ui.home.folderLabel
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
    var showMove by remember { mutableStateOf(false) }
    var pendingLink by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(state.finished) { if (state.finished) onBack() }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (state.message != null) {
                ReaderActionBar(
                    onReply = { onCompose("reply", vm.messageId) },
                    onReplyAll = { onCompose("replyall", vm.messageId) },
                    onForward = { onCompose("forward", vm.messageId) },
                    onArchive = vm::archive,
                    onDelete = vm::delete,
                )
            }
        },
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
                        IconButton(onClick = vm::markUnread) {
                            Icon(Icons.Outlined.MarkEmailUnread, contentDescription = stringResource(R.string.reader_mark_unread))
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.reader_more))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.move_to)) },
                                    leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                                    onClick = { menuOpen = false; showMove = true },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(
                                                if (state.darkView) R.string.reader_original_colors else R.string.reader_dark_view,
                                            ),
                                        )
                                    },
                                    onClick = { menuOpen = false; vm.setDarkView(!state.darkView) },
                                )
                                if (state.blockedRemote > 0 || state.allowRemote) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(
                                                    if (state.allowRemote) R.string.reader_hide_images else R.string.reader_show_images,
                                                ),
                                            )
                                        },
                                        onClick = { menuOpen = false; vm.setAllowRemote(!state.allowRemote) },
                                    )
                                }
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
                s.loading -> ReaderSkeleton()
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
                    notice = s.notice?.let { actionNoticeText(it) },
                    message = message,
                    html = s.html,
                    starred = s.starred,
                    darkView = s.darkView,
                    blockedRemote = s.blockedRemote,
                    allowRemote = s.allowRemote,
                    onToggleStar = vm::toggleStar,
                    onShowImages = { vm.setAllowRemote(true) },
                    onLink = { pendingLink = it },
                )
            }
        }
    }

    if (showMove) {
        AlertDialog(
            onDismissRequest = { showMove = false },
            title = { Text(stringResource(R.string.move_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    state.folders.forEach { f ->
                        TextButton(onClick = { showMove = false; vm.moveTo(f) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(folderIcon(f.role), contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(folderLabel(f), modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showMove = false }) { Text(stringResource(R.string.reader_link_cancel)) } },
        )
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
    notice: String?,
    message: ParsedMessage,
    html: String,
    starred: Boolean,
    darkView: Boolean,
    blockedRemote: Int,
    allowRemote: Boolean,
    onToggleStar: () -> Unit,
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
        if (notice != null) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    notice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        ReaderHeader(
            message = message,
            starred = starred,
            onToggleStar = onToggleStar,
            showImagesBanner = blockedRemote > 0 && !allowRemote,
            onShowImages = onShowImages,
            onSave = {
                pending = it
                saveLauncher.launch(it.fileName)
            },
        )
        // The email is its own page. With the dark view on, the page is black to match the app.
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = if (darkView) Color.Black else Color.White,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            EmailWebView(
                html = html,
                allowRemote = allowRemote,
                dark = darkView,
                onLink = onLink,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
@Composable
private fun ReaderHeader(
    message: ParsedMessage,
    starred: Boolean,
    onToggleStar: () -> Unit,
    showImagesBanner: Boolean,
    onShowImages: () -> Unit,
    onSave: (ParsedAttachment) -> Unit,
) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val from = message.from
    val senderName = from?.name ?: from?.address ?: stringResource(R.string.home_unknown_sender)
    val shortDate = message.dateMillis?.let {
        DateUtils.formatDateTime(
            context, it,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
        )
    }.orEmpty()
    val fullDate = message.dateMillis?.let {
        DateUtils.formatDateTime(
            context, it,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR,
        )
    }

    Column(Modifier.padding(start = 20.dp, end = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = message.subject.ifBlank { stringResource(R.string.home_no_subject) },
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(top = 6.dp),
            )
            IconButton(onClick = onToggleStar) {
                Icon(
                    imageVector = if (starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(R.string.home_toggle_star),
                    tint = if (starred) Color(0xFFF4B400) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            Modifier.padding(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(AvatarColors.colorFor(from?.address ?: senderName)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    senderName.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        senderName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(shortDate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    Modifier.clickable(onClick = { expanded = !expanded }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        recipientSummary(message.to),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(if (expanded) R.string.reader_details_hide else R.string.reader_details_show),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        AnimatedVisibility(expanded) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.padding(end = 12.dp).fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DetailLine(stringResource(R.string.reader_label_from), from?.display())
                    DetailLine(stringResource(R.string.reader_label_to), message.to.joinToString(", ") { it.display() }.ifEmpty { null })
                    DetailLine(stringResource(R.string.reader_label_cc), message.cc.joinToString(", ") { it.display() }.ifEmpty { null })
                    DetailLine(stringResource(R.string.reader_label_date), fullDate)
                    DetailLine(stringResource(R.string.reader_label_id), message.messageId)
                }
            }
        }
        if (showImagesBanner) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(end = 12.dp),
            ) {
                Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
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
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                message.attachments.forEach { AttachmentCard(it) { onSave(it) } }
            }
        }
    }
}

@Composable
private fun AttachmentCard(attachment: ParsedAttachment, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon = when (AttachmentKinds.of(attachment.fileName, attachment.mimeType)) {
        AttachmentKind.IMAGE -> Icons.Outlined.Image
        AttachmentKind.PDF -> Icons.Outlined.PictureAsPdf
        AttachmentKind.DOCUMENT -> Icons.Outlined.Description
        AttachmentKind.ARCHIVE -> Icons.Outlined.FolderZip
        AttachmentKind.OTHER -> Icons.Outlined.InsertDriveFile
    }
    Surface(onClick = onClick, shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(
                    attachment.fileName,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp),
                )
                Text(
                    Formatter.formatShortFileSize(context, attachment.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Outlined.Download,
                contentDescription = stringResource(R.string.reader_save_attachment, attachment.fileName),
                modifier = Modifier.size(20.dp),
            )
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
        Text(
            label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(56.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ReaderActionBar(
    onReply: () -> Unit,
    onReplyAll: () -> Unit,
    onForward: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onReply) {
                Icon(Icons.AutoMirrored.Outlined.Reply, contentDescription = stringResource(R.string.compose_reply))
            }
            IconButton(onClick = onReplyAll) {
                Icon(Icons.AutoMirrored.Outlined.ReplyAll, contentDescription = stringResource(R.string.compose_reply_all))
            }
            IconButton(onClick = onForward) {
                Icon(Icons.AutoMirrored.Outlined.Forward, contentDescription = stringResource(R.string.compose_forward))
            }
            IconButton(onClick = onArchive) {
                Icon(Icons.Outlined.Archive, contentDescription = stringResource(R.string.home_archive))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.home_delete))
            }
        }
    }
}

/** Placeholder shown while the message is fetched or parsed. */
@Composable
private fun ReaderSkeleton() {
    val transition = rememberInfiniteTransition(label = "reader-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    val tint = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.height(22.dp).fillMaxWidth(0.7f).clip(RoundedCornerShape(8.dp)).background(tint))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(tint))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.height(12.dp).width(140.dp).clip(RoundedCornerShape(6.dp)).background(tint))
                Box(Modifier.height(10.dp).width(100.dp).clip(RoundedCornerShape(5.dp)).background(tint))
            }
        }
        Spacer(Modifier.height(8.dp))
        repeat(7) { i ->
            Box(Modifier.height(12.dp).fillMaxWidth(if (i % 3 == 2) 0.55f else 1f).clip(RoundedCornerShape(6.dp)).background(tint))
        }
    }
}

@Composable
private fun EmailWebView(
    html: String,
    allowRemote: Boolean,
    dark: Boolean,
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
            view.setBackgroundColor(if (dark) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            view.settings.blockNetworkLoads = !allowRemote
            if (view.tag != html) {
                view.tag = html
                view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
        onRelease = { it.destroy() },
    )
}
