package com.kmail.thekami.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kmail.thekami.R
import com.kmail.thekami.data.repo.MessageActionResult
import com.kmail.thekami.domain.model.FolderRole
import com.kmail.thekami.ui.setup.messageRes

private fun roleName(role: FolderRole) = when (role) {
    FolderRole.INBOX -> R.string.role_inbox
    FolderRole.SENT -> R.string.role_sent
    FolderRole.DRAFTS -> R.string.role_drafts
    FolderRole.ARCHIVE -> R.string.role_archive
    FolderRole.SPAM -> R.string.role_spam
    FolderRole.TRASH -> R.string.role_trash
    FolderRole.CUSTOM -> R.string.role_inbox
}

/** Text for a failed action, or null if it succeeded. */
@Composable
fun actionNoticeText(result: MessageActionResult): String? = when (result) {
    MessageActionResult.Ok -> null
    is MessageActionResult.NoTargetFolder ->
        stringResource(R.string.home_notice_no_folder, stringResource(roleName(result.role)))
    is MessageActionResult.Failed -> stringResource(
        R.string.home_action_error,
        result.error?.let { stringResource(it.messageRes()) } ?: stringResource(R.string.error_unknown),
    )
}
