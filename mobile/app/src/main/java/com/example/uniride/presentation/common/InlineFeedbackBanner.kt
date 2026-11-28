package com.example.uniride.presentation.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme

/** Inline error or confirmation banner shared across form screens (TalkBack polite live region). */
@Composable
fun InlineFeedbackBanner(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(Dimens.SpaceM),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InlineFeedbackBannerErrorPreview() {
    UniRideTheme {
        InlineFeedbackBanner(message = "Correo o contraseña incorrectos.", isError = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun InlineFeedbackBannerSuccessPreview() {
    UniRideTheme {
        InlineFeedbackBanner(message = "Perfil actualizado correctamente.", isError = false)
    }
}
