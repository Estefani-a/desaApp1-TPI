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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniride.R
import com.example.uniride.presentation.common.LocalAppSnackbar
import com.example.uniride.presentation.theme.UniRideTheme

/**
 * Contextual `POST_NOTIFICATIONS` request (RNF08, Android 13+).
 *
 * Shown only when the user actually has a reason to receive pushes (after requesting a seat on B5
 * or after publishing a trip on C1), never on launch or login. Declining never blocks any flow,
 * communicates that status remains visible inside the app (CONVENCIONES §7.7), and is persisted
 * at device level so the prompt is not repeated.
 */
@Composable
fun NotificationPermissionPrompt(
    @StringRes rationaleRes: Int,
    shouldPrompt: Boolean = true,
    viewModel: NotificationPermissionViewModel = hiltViewModel(),
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val resources = LocalResources.current
    val appSnackbar = LocalAppSnackbar.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> viewModel.onAction(NotificationPermissionAction.SystemResult(granted)) },
    )

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                NotificationPermissionEffect.LaunchSystemRequest ->
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                is NotificationPermissionEffect.ShowMessage ->
                    appSnackbar?.show(resources.getString(effect.messageRes))
            }
        }
    }

    val isGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED

    if (shouldPrompt && !isGranted && state.hasPrompted == false) {
        NotificationPermissionDialog(
            rationaleRes = rationaleRes,
            onConfirm = { viewModel.onAction(NotificationPermissionAction.Confirm) },
            onDismiss = { viewModel.onAction(NotificationPermissionAction.Dismiss) },
        )
    }
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
