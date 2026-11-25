package com.example.uniride.presentation.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.uniride.R
import com.example.uniride.presentation.theme.UniRideTheme

/**
 * Top bar of the screens. With [onBack] it shows the back arrow; the tabs of the bottom bar
 * do not need it.
 *
 * @param windowInsets `WindowInsets(0)` inside the shell of the bottom bar, which already leaves
 * the space of the status bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniRideTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.common_back),
                    )
                }
            }
        },
        actions = actions,
        windowInsets = windowInsets,
    )
}

@Preview(showBackground = true)
@Composable
private fun UniRideTopBarPreview() {
    UniRideTheme { UniRideTopBar(title = "Detalle del viaje", onBack = {}) }
}
