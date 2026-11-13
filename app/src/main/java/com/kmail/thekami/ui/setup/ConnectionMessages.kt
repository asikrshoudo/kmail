package com.kmail.thekami.ui.setup

import androidx.annotation.StringRes
import com.kmail.thekami.R
import com.kmail.thekami.domain.mail.MailError
import com.kmail.thekami.domain.model.SuggestionNote

@StringRes
fun MailError.messageRes(): Int = when (this) {
    is MailError.Authentication -> R.string.error_auth
    is MailError.Tls -> R.string.error_tls
    is MailError.Timeout -> R.string.error_timeout
    is MailError.Network -> R.string.error_network
    is MailError.Protocol -> R.string.error_protocol
    is MailError.Unsupported -> R.string.error_unknown
    is MailError.Unknown -> R.string.error_unknown
}

@StringRes
fun ConnectFailure.titleRes(): Int = when (this) {
    is ConnectFailure.Incoming -> R.string.error_incoming_title
    is ConnectFailure.Outgoing -> R.string.error_outgoing_title
    else -> R.string.error_generic_title
}

@StringRes
fun ConnectFailure.messageRes(): Int = when (this) {
    is ConnectFailure.Incoming -> this.error.messageRes()
    is ConnectFailure.Outgoing -> this.error.messageRes()
    ConnectFailure.InvalidInput -> R.string.error_invalid_input
    ConnectFailure.AlreadyExists -> R.string.error_already_exists
    ConnectFailure.StorageFailed -> R.string.error_storage
}

@StringRes
fun SuggestionNote.hintRes(): Int? = when (this) {
    SuggestionNote.NONE -> null
    SuggestionNote.APP_PASSWORD_REQUIRED -> R.string.note_app_password
    SuggestionNote.OAUTH_MAY_BE_REQUIRED -> R.string.note_oauth
    SuggestionNote.GUESSED -> R.string.note_guessed
}
