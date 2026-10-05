package com.smartmeasure.ar.presentation.diagnostics

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartmeasure.ar.R
import com.smartmeasure.ar.domain.model.ArAvailability
import com.smartmeasure.ar.domain.model.ArPreparationFailure
import com.smartmeasure.ar.domain.model.DepthSupport
import com.smartmeasure.ar.domain.model.PlaneFindingSupport

@Composable
fun DiagnosticsScreen(
    uiState: DiagnosticsUiState,
    onRefresh: () -> Unit,
    onPrepareAr: () -> Unit,
    onOpenManualMode: () -> Unit,
    onOpenFieldTrials: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.diagnostics_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.diagnostics_subtitle),
                style = MaterialTheme.typography.bodyLarge,
            )

            DiagnosticCard(
                label = stringResource(R.string.device_label),
                value = Build.MANUFACTURER + " " + Build.MODEL,
                detail = stringResource(R.string.android_version_format, Build.VERSION.SDK_INT),
            )
            DiagnosticCard(
                label = stringResource(R.string.arcore_label),
                value = arAvailabilityText(uiState.arAvailability),
            )
            DiagnosticCard(
                label = stringResource(R.string.depth_label),
                value = depthSupportText(uiState.depthSupport),
            )
            DiagnosticCard(
                label = stringResource(R.string.plane_detection_label),
                value = planeFindingText(uiState.planeFindingSupport),
            )
            DiagnosticCard(
                label = stringResource(R.string.fallback_label),
                value = stringResource(R.string.fallback_available),
            )

            PreparationFeedback(uiState)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onPrepareAr,
                    enabled = uiState.canPrepareAr &&
                        uiState.preparationState != PreparationState.PREPARING,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.prepare_ar))
                }
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.refresh))
                }
            }

            OutlinedButton(
                onClick = onOpenManualMode,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.open_manual_mode))
            }
            OutlinedButton(
                onClick = onOpenFieldTrials,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.open_field_trials))
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.phase_zero_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DiagnosticCard(
    label: String,
    value: String,
    detail: String? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PreparationFeedback(uiState: DiagnosticsUiState) {
    when (uiState.preparationState) {
        PreparationState.IDLE,
        PreparationState.READY,
        -> Unit

        PreparationState.PREPARING -> Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator()
            Text(stringResource(R.string.preparing))
        }

        PreparationState.INSTALL_REQUESTED ->
            Text(stringResource(R.string.install_requested))

        PreparationState.PERMISSION_DENIED ->
            Text(stringResource(R.string.permission_denied))

        PreparationState.FAILED ->
            Text(failureText(uiState.failure))
    }
}

@Composable
private fun arAvailabilityText(availability: ArAvailability): String = stringResource(
    when (availability) {
        ArAvailability.CHECKING -> R.string.ar_checking
        ArAvailability.READY -> R.string.ar_ready
        ArAvailability.NEEDS_INSTALL_OR_UPDATE -> R.string.ar_needs_install
        ArAvailability.UNSUPPORTED -> R.string.ar_unsupported
        ArAvailability.UNKNOWN -> R.string.ar_unknown
    },
)

@Composable
private fun depthSupportText(depthSupport: DepthSupport): String = stringResource(
    when (depthSupport) {
        DepthSupport.NOT_CHECKED -> R.string.depth_not_checked
        DepthSupport.SUPPORTED -> R.string.depth_supported
        DepthSupport.UNSUPPORTED -> R.string.depth_unsupported
    },
)

@Composable
private fun planeFindingText(planeFindingSupport: PlaneFindingSupport): String = stringResource(
    when (planeFindingSupport) {
        PlaneFindingSupport.NOT_CHECKED -> R.string.planes_not_checked
        PlaneFindingSupport.HORIZONTAL_AND_VERTICAL -> R.string.planes_ready
    },
)

@Composable
private fun failureText(failure: ArPreparationFailure?): String = stringResource(
    when (failure) {
        ArPreparationFailure.DEVICE_INCOMPATIBLE -> R.string.ar_failed_incompatible
        ArPreparationFailure.ARCORE_NOT_INSTALLED -> R.string.ar_failed_not_installed
        ArPreparationFailure.ARCORE_TOO_OLD -> R.string.ar_failed_apk_too_old
        ArPreparationFailure.SDK_TOO_OLD -> R.string.ar_failed_sdk_too_old
        ArPreparationFailure.INSTALL_DECLINED -> R.string.ar_failed_install_declined
        ArPreparationFailure.CAMERA_UNAVAILABLE -> R.string.ar_failed_camera
        ArPreparationFailure.CONFIGURATION_UNSUPPORTED -> R.string.ar_failed_configuration
        ArPreparationFailure.UNKNOWN,
        null,
        -> R.string.ar_failed_unknown
    },
)
