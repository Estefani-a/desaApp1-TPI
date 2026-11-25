package com.example.uniride.presentation.publish

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniride.R
import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.CampusCode
import com.example.uniride.presentation.common.ContentState
import com.example.uniride.presentation.common.ContentStateHost
import com.example.uniride.presentation.common.EmptyState
import com.example.uniride.presentation.common.LocalAppSnackbar
import com.example.uniride.presentation.common.OfflineBanner
import com.example.uniride.presentation.common.PrimaryButton
import com.example.uniride.presentation.common.UniRideTopBar
import com.example.uniride.presentation.publish.components.PublishTripForm
import com.example.uniride.presentation.theme.Dimens
import com.example.uniride.presentation.theme.UniRideTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Gets the [PublishTripViewModel] and turns its effects into Snackbars and navigation. After
 * publishing, the message goes to the Snackbar of the shell and the screen goes back at once,
 * so the driver does not stay looking at a locked form.
 */
@Composable
fun PublishTripRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PublishTripViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val appSnackbar = LocalAppSnackbar.current
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PublishTripEffect.Published -> {
                    val message = resources.getString(effect.messageRes)
                    if (appSnackbar != null) {
                        appSnackbar.show(message)
                    } else {
                        scope.launch { snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short) }
                    }
                    onBack()
                }
                is PublishTripEffect.ShowMessage -> snackbarHostState.showSnackbar(
                    resources.getString(effect.messageRes),
                )
            }
        }
    }

    PublishTripScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/**
 * C2 Publicar un viaje (RF02): guided form with the button "Publicar viaje" at the bottom. Without
 * connection the form keeps working under the "Sin conexión" banner, because the trip is saved on
 * the phone and sent later.
 */
@Composable
fun PublishTripScreen(
    state: PublishTripUiState,
    onAction: (PublishTripAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    // The shell with the bottom bar already leaves the space of the system bars
    Scaffold(
        modifier = modifier,
        topBar = {
            UniRideTopBar(
                title = stringResource(R.string.publish_title),
                onBack = onBack,
                windowInsets = WindowInsets(0),
            )
        },
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isOffline) {
                OfflineBanner(lastUpdatedAt = null)
            }
            ContentStateHost(
                state = state.campuses,
                modifier = Modifier.weight(1f),
                onRetry = { onAction(PublishTripAction.RetryCampuses) },
                empty = {
                    EmptyState(
                        title = stringResource(R.string.publish_campuses_empty_title),
                        message = stringResource(R.string.publish_campuses_empty_message),
                    )
                },
            ) { campuses ->
                FormWithButton(state = state, campuses = campuses, onAction = onAction)
            }
        }
    }
}

/** The scrolling form and, below it and always visible, the main button. */
@Composable
private fun FormWithButton(
    state: PublishTripUiState,
    campuses: List<Campus>,
    onAction: (PublishTripAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        PublishTripForm(
            state = state,
            campuses = campuses,
            onAction = onAction,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceL),
        )
        PrimaryButton(
            text = stringResource(R.string.publish_submit),
            onClick = { onAction(PublishTripAction.Submit) },
            isLoading = state.isSubmitting,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpaceM),
        )
    }
}

private val previewCampuses = listOf(
    Campus(CampusCode("MONSERRAT"), "Sede Monserrat", listOf("Salida Lima 717", "Av. Independencia 1000")),
    Campus(CampusCode("BELGRANO"), "Sede Belgrano", listOf("Puerta Zapiola")),
)

private val previewForm = PublishTripForm(
    campus = CampusCode("MONSERRAT"),
    meetingPoint = "Salida Lima 717",
    neighborhood = "Ramos Mejía",
    reference = "Estación de tren",
    departureDate = LocalDate.now().plusDays(1),
    departureTime = LocalTime.of(22, 15),
    arrivalTime = LocalTime.of(23, 0),
    contribution = "3500",
)

private fun previewState(
    form: PublishTripForm = previewForm,
    campuses: ContentState<List<Campus>> = ContentState.Success(previewCampuses),
    errors: Map<PublishField, Int> = emptyMap(),
    isSubmitting: Boolean = false,
    isOffline: Boolean = false,
) = PublishTripUiState(
    campuses = campuses,
    form = form,
    errors = errors,
    isSubmitting = isSubmitting,
    isOffline = isOffline,
)

@Preview(showBackground = true, name = "Cargando sedes")
@Composable
private fun PublishTripScreenLoadingPreview() {
    UniRideTheme {
        PublishTripScreen(state = previewState(campuses = ContentState.Loading), onAction = {}, onBack = {})
    }
}

@Preview(showBackground = true, name = "Formulario vacío")
@Composable
private fun PublishTripScreenEmptyFormPreview() {
    UniRideTheme {
        PublishTripScreen(
            state = previewState(form = PublishTripForm(departureDate = LocalDate.now())),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, name = "Formulario completo")
@Composable
private fun PublishTripScreenFilledPreview() {
    UniRideTheme { PublishTripScreen(state = previewState(), onAction = {}, onBack = {}) }
}

@Preview(showBackground = true, name = "Con errores")
@Composable
private fun PublishTripScreenErrorsPreview() {
    UniRideTheme {
        PublishTripScreen(
            state = previewState(
                form = PublishTripForm(departureDate = LocalDate.now()),
                errors = mapOf(
                    PublishField.CAMPUS to R.string.publish_error_campus,
                    PublishField.MEETING_POINT to R.string.publish_error_meeting_point,
                    PublishField.NEIGHBORHOOD to R.string.publish_error_neighborhood,
                    PublishField.DEPARTURE to R.string.publish_error_departure_missing,
                    PublishField.CONTRIBUTION to R.string.publish_error_contribution,
                ),
            ),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, name = "Publicando")
@Composable
private fun PublishTripScreenSubmittingPreview() {
    UniRideTheme {
        PublishTripScreen(state = previewState(isSubmitting = true), onAction = {}, onBack = {})
    }
}

@Preview(showBackground = true, name = "Sin conexión con formulario")
@Composable
private fun PublishTripScreenOfflinePreview() {
    UniRideTheme {
        PublishTripScreen(state = previewState(isOffline = true), onAction = {}, onBack = {})
    }
}

@Preview(showBackground = true, name = "Sin sedes y sin conexión")
@Composable
private fun PublishTripScreenNoCampusesOfflinePreview() {
    UniRideTheme {
        PublishTripScreen(
            state = previewState(
                campuses = ContentState.Error(R.string.publish_campuses_offline),
                isOffline = true,
            ),
            onAction = {},
            onBack = {},
        )
    }
}
