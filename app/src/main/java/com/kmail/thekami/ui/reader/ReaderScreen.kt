package com.kmail.thekami.ui.reader

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.View
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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmail.thekami.R
import com.kmail.thekami.domain.mail.MailError
import com.kmail.thekami.mail.Mailbox
import com.kmail.thekami.mail.ParsedAttachment
import com.kmail.thekami.mail.ParsedMessage
import com.kmail.thekami.ui.components.ErrorState
import com.kmail.thekami.ui.home.AvatarColors
import com.kmail.thekami.ui.home.actionNoticeText
import com.kmail.thekami.ui.home.folderIcon
import com.kmail.thekami.ui.home.folderLabel
import com.kmail.thekami.ui.setup.messageRes
import java.util.Date
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var showMove by remember { mutableStateOf(false) }
    var pendingLink by remember { mutableStateOf<String?>(null) }
    var pendingAttachment by remember { mutableStateOf<ParsedAttachment?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val attachment = pendingAttachment
        pendingAttachment = null
        if (uri != null && attachment != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(attachment.bytes) }
                            ?: error("no stream")
                    }.isSuccess
                }
                Toast.makeText(context, if (ok) R.string.reader_saved else R.string.reader_save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(state.finished) { if (state.finished) onBack() }
    val openedBody = state.bodies[state.openedId]

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    if (state.thread.size > 1) {
                        Text(
                            pluralStringResource(R.plurals.reader_thread_count, state.thread.size, state.thread.size),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.reader_back))
                    }
                },
                actions = {
                    if (state.thread.isNotEmpty()) {
                        IconButton(onClick = vm::archive) {
                            Icon(Icons.Outlined.Archive, contentDescription = stringResource(R.string.home_archive))
                        }
                        IconButton(onClick = vm::delete) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.home_delete))
                        }
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
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (openedBody != null) {
                val recipients = openedBody.message.to.size + openedBody.message.cc.size
                ReplyBar(
                    showReplyAll = recipients > 1,
                    onReply = { onCompose("reply", vm.messageId) },
                    onReplyAll = { onCompose("replyall", vm.messageId) },
                    onForward = { onCompose("forward", vm.messageId) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val error = state.error
            when {
                state.missing -> Text(
                    stringResource(R.string.reader_missing),
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
                error != null && openedBody == null -> ErrorState(
                    title = stringResource(R.string.reader_error_title),
                    message = stringResource(error.messageRes()),
                    retryLabel = stringResource(R.string.connecting_retry),
                    onRetry = vm::load,
                )
                state.thread.isEmpty() -> ReaderSkeleton()
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    state.notice?.let { notice ->
                        actionNoticeText(notice)?.let {
                            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                    Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.Top) {
                        Text(
                            text = state.subject.ifBlank { stringResource(R.string.home_no_subject) },
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f).padding(top = 6.dp),
                        )
                        IconButton(onClick = vm::toggleStar) {
                            Icon(
                                imageVector = if (state.starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = stringResource(R.string.home_toggle_star),
                                tint = if (state.starred) Color(0xFFF4B400) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    state.openedFolder?.let { folder ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 12.dp),
                        ) {
                            Text(
                                folderLabel(folder),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.thread.forEach { item ->
                            ThreadCard(
                                item = item,
                                expanded = item.id in state.expanded,
                                body = state.bodies[item.id],
                                loading = item.id in state.loadingBodies,
                                failed = state.failedBodies[item.id],
                                dark = state.darkView,
                                ownEmail = state.ownEmail,
                                onToggle = { vm.toggle(item.id) },
                                onCompose = { mode -> onCompose(mode, item.id) },
                                onShowPictures = { vm.setAllowRemote(item.id, true) },
                                onSave = { attachment ->
                                    pendingAttachment = attachment
                                    saveLauncher.launch(attachment.fileName)
                                },
                                onLink = { pendingLink = it },
                                onRetry = { vm.retryBody(item.id) },
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
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
private fun shortDate(millis: Long): String {
    if (millis <= 0) return ""
    val context = LocalContext.current
    return if (DateUtils.isToday(millis)) {
        DateFormat.getTimeFormat(context).format(Date(millis))
    } else {
        DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
    }
}

@Composable
private fun toLine(to: List<Mailbox>, ownEmail: String): String {
    if (to.isEmpty()) return stringResource(R.string.reader_no_recipients)
    val me = stringResource(R.string.reader_me)
    val names = to.map { if (ownEmail.isNotBlank() && it.address.equals(ownEmail, true)) me else (it.name ?: it.address) }
    return stringResource(R.string.reader_to_line, names.joinToString(", "))
}

@Composable
private fun ThreadCard(
    item: ThreadItem,
    expanded: Boolean,
    body: LoadedBody?,
    loading: Boolean,
    failed: MailError?,
    dark: Boolean,
    ownEmail: String,
    onToggle: () -> Unit,
    onCompose: (String) -> Unit,
    onShowPictures: () -> Unit,
    onSave: (ParsedAttachment) -> Unit,
    onLink: (String) -> Unit,
    onRetry: () -> Unit,
) {
    var showDetails by rememberSaveable(item.id) { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    val name = if (item.isOwn) stringResource(R.string.reader_me_cap) else item.senderName.ifBlank { stringResource(R.string.home_unknown_sender) }
    val message = body?.message

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = if (expanded) 6.dp else 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(AvatarColors.colorFor(item.senderAddress.ifBlank { name })),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        name.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (!item.isRead && !expanded) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (expanded) {
                        Text(
                            item.senderAddress,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(Modifier.clickable { showDetails = !showDetails }, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                message?.let { toLine(it.to, ownEmail) } ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Icon(
                                if (showDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = stringResource(if (showDetails) R.string.reader_details_hide else R.string.reader_details_show),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else if (item.snippet.isNotBlank()) {
                        Text(
                            item.snippet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        shortDate(item.receivedAtMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (expanded) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedIconButton(onClick = { onCompose("reply") }, modifier = Modifier.size(40.dp), shape = RoundedCornerShape(14.dp)) {
                                Icon(Icons.AutoMirrored.Outlined.Reply, contentDescription = stringResource(R.string.compose_reply), modifier = Modifier.size(20.dp))
                            }
                            Box {
                                OutlinedIconButton(onClick = { moreOpen = true }, modifier = Modifier.size(40.dp), shape = RoundedCornerShape(14.dp)) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.reader_more), modifier = Modifier.size(20.dp))
                                }
                                DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.compose_reply_all)) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ReplyAll, contentDescription = null) },
                                        onClick = { moreOpen = false; onCompose("replyall") },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.compose_forward)) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Forward, contentDescription = null) },
                                        onClick = { moreOpen = false; onCompose("forward") },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(expanded) {
                Column(Modifier.fillMaxWidth()) {
                    if (showDetails && message != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                DetailLine(stringResource(R.string.reader_label_from), message.from?.display())
                                DetailLine(stringResource(R.string.reader_label_to), message.to.joinToString(", ") { it.display() }.ifEmpty { null })
                                DetailLine(stringResource(R.string.reader_label_cc), message.cc.joinToString(", ") { it.display() }.ifEmpty { null })
                                DetailLine(
                                    stringResource(R.string.reader_label_date),
                                    message.dateMillis?.let {
                                        DateUtils.formatDateTime(
                                            LocalContext.current, it,
                                            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR,
                                        )
                                    },
                                )
                                DetailLine(stringResource(R.string.reader_label_id), message.messageId)
                            }
                        }
                    }
                    message?.unsubscribe?.let { link ->
                        TextButton(onClick = { onLink(link) }, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(stringResource(R.string.reader_unsubscribe))
                        }
                    }
                    if (body != null && body.blockedRemote > 0 && !body.allowRemote) {
                        Surface(
                            onClick = onShowPictures,
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Icon(Icons.Outlined.Image, contentDescription = null)
                                Text(stringResource(R.string.reader_show_pictures), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    if (message != null && message.attachments.isNotEmpty()) {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            message.attachments.forEach { AttachmentCard(it) { onSave(it) } }
                        }
                    }
                    when {
                        body != null -> EmailWebView(
                            html = body.html,
                            allowRemote = body.allowRemote,
                            dark = dark,
                            onLink = onLink,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(bottom = 6.dp),
                        )
                        failed != null -> Column(Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.reader_body_failed), color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onRetry) { Text(stringResource(R.string.connecting_retry)) }
                        }
                        loading -> BodySkeleton()
                    }
                }
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
private fun ReplyBar(
    showReplyAll: Boolean,
    onReply: () -> Unit,
    onReplyAll: () -> Unit,
    onForward: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(onClick = onReply, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Outlined.Reply, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.compose_reply), maxLines = 1)
            }
            if (showReplyAll) {
                FilledTonalButton(onClick = onReplyAll, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Outlined.ReplyAll, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.compose_reply_all), maxLines = 1)
                }
            }
            FilledTonalButton(onClick = onForward, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Outlined.Forward, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.compose_forward), maxLines = 1)
            }
        }
    }
}

@Composable
private fun skeletonTint(): Color {
    val transition = rememberInfiniteTransition(label = "reader-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    return MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)
}

@Composable
private fun BodySkeleton() {
    val tint = skeletonTint()
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(4) { i ->
            Box(Modifier.height(12.dp).fillMaxWidth(if (i == 3) 0.5f else 1f).clip(RoundedCornerShape(6.dp)).background(tint))
        }
    }
}

/** Shown while the thread is being found. */
@Composable
private fun ReaderSkeleton() {
    val tint = skeletonTint()
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.height(24.dp).fillMaxWidth(0.7f).clip(RoundedCornerShape(8.dp)).background(tint))
        Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(20.dp)).background(tint))
    }
}

/**
 * The email page is sized by its content and scrolls together with the rest of the screen.
 * JavaScript is off and files are not reachable.
 */
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
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                isNestedScrollingEnabled = true
                overScrollMode = View.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
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
            view.setBackgroundColor(if (dark) android.graphics.Color.TRANSPARENT else android.graphics.Color.WHITE)
            view.settings.blockNetworkLoads = !allowRemote
            if (view.tag != html) {
                view.tag = html
                view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
        onRelease = { it.destroy() },
    )
}
