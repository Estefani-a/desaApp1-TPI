package com.example.uniride.presentation.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme

/**
 * Main action of a screen, at the bottom and full width (RNF01: in the lower half, at least
 * 48 dp). While [isLoading] it shows a spinner and ignores taps, so a form is not sent twice.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimens.IconMedium),
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonPreview() {
    UniRideTheme { PrimaryButton(text = "Solicitar asiento", onClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonLoadingPreview() {
    UniRideTheme { PrimaryButton(text = "Solicitar asiento", onClick = {}, isLoading = true) }
}
