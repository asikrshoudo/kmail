package com.kmail.thekami.ui.setup

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kmail.thekami.R
import com.kmail.thekami.ui.components.ErrorState
import com.kmail.thekami.ui.components.LoadingState

@Composable
fun ConnectingScreen(
    state: ConnectState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is ConnectState.Failed) {
        ErrorState(
            title = stringResource(state.failure.titleRes()),
            message = stringResource(state.failure.messageRes()),
            retryLabel = stringResource(R.string.connecting_retry),
            onRetry = onRetry,
            modifier = modifier,
        )
        return
    }
    val intro = stringResource(R.string.connecting_message)
    val stage = (state as? ConnectState.Testing)?.stage?.let {
        stringResource(
            when (it) {
                ConnectStage.INCOMING -> R.string.connecting_incoming
                ConnectStage.OUTGOING -> R.string.connecting_outgoing
                ConnectStage.SAVING -> R.string.connecting_saving
            },
        )
    }
    LoadingState(
        message = if (stage == null) intro else intro + "\n\n" + stage,
        modifier = modifier,
    )
}
