package com.example.uniride.presentation.push

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.uniride.R
import com.example.uniride.domain.usecase.push.MarkNotificationPermissionPromptedUseCase
import com.example.uniride.domain.usecase.push.ObserveNotificationPermissionPromptedUseCase
import com.example.uniride.presentation.theme.UniRideTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Holds whether the contextual `POST_NOTIFICATIONS` prompt (RNF08) has already been shown. */
@HiltViewModel
class NotificationPermissionViewModel @Inject constructor(
    observePrompted: ObserveNotificationPermissionPromptedUseCase,
    private val markPrompted: MarkNotificationPermissionPromptedUseCase,
) : ViewModel() {

    /** `null` while DataStore loads so the rationale dialog never flashes unnecessarily. */
    val hasPrompted: StateFlow<Boolean?> = observePrompted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onPromptHandled() {
        viewModelScope.launch { markPrompted() }
    }
}

/**
 * Contextual `POST_NOTIFICATIONS` request (RNF08, Android 13+).
 *
 * Shown only when the user actually has a reason to receive pushes (after requesting a seat on B5
 * or after publishing a trip on C1), never on launch or login. Declining never blocks any flow
 * and the prompt is not repeated.
 */
@Composable
fun NotificationPermissionPrompt(
    @StringRes rationaleRes: Int,
    shouldPrompt: Boolean = true,
    viewModel: NotificationPermissionViewModel = hiltViewModel(),
) {
    if (!shouldPrompt || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val isGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
    if (isGranted) return

    val hasPrompted by viewModel.hasPrompted.collectAsStateWithLifecycle()
    if (hasPrompted != false) return

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ -> },
    )

    NotificationPermissionDialog(
        rationaleRes = rationaleRes,
        onConfirm = {
            viewModel.onPromptHandled()
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        onDismiss = viewModel::onPromptHandled,
    )
}

@Composable
internal fun NotificationPermissionDialog(
    @StringRes rationaleRes: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.push_permission_title)) },
        text = { Text(stringResource(rationaleRes)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.push_permission_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.push_permission_not_now))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun NotificationPermissionPassengerPreview() {
    UniRideTheme {
        NotificationPermissionDialog(
            rationaleRes = R.string.push_permission_passenger_rationale,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationPermissionDriverPreview() {
    UniRideTheme {
        NotificationPermissionDialog(
            rationaleRes = R.string.push_permission_driver_rationale,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
