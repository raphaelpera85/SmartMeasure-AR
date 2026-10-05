package com.smartmeasure.ar.presentation.ar

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.smartmeasure.ar.R
import java.util.Locale

@Composable
fun ArMeasurementScreen(
    uiState: ArMeasurementUiState,
    listener: ArMeasureView.Listener,
    onResetState: () -> Unit,
    onRecordTrial: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity: Activity = requireNotNull(LocalActivity.current) {
        "AR screen must be hosted by an Activity."
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val arView = remember(activity) { ArMeasureView(activity, listener) }

    DisposableEffect(lifecycleOwner, arView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> arView.onHostResume()
                Lifecycle.Event.ON_PAUSE -> arView.onHostPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            arView.onHostResume()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            arView.close()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { arView },
            modifier = Modifier.fillMaxSize(),
        )

        Text(
            text = "+",
            color = Color.White,
            fontSize = 44.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.align(Alignment.Center),
        )

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.ar_measurement_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(messageText(uiState.message))
                Text(
                    text = if (uiState.depthEnabled) {
                        stringResource(R.string.ar_depth_active)
                    } else {
                        stringResource(R.string.ar_depth_inactive)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                uiState.distanceMeters?.let { distance ->
                    Text(
                        text = stringResource(
                            R.string.ar_distance_result,
                            formatMeters(distance),
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = arView::capturePoint,
                enabled = uiState.sessionReady && uiState.tracking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (uiState.capturedPoints == 1) {
                        stringResource(R.string.capture_second_point)
                    } else {
                        stringResource(R.string.capture_first_point)
                    },
                )
            }
            if (uiState.distanceMeters != null) {
                OutlinedButton(
                    onClick = onRecordTrial,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.record_trial))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        arView.resetMeasurement()
                        onResetState()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.reset_measurement))
                }
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.back))
                }
            }
        }
    }
}

@Composable
private fun messageText(message: ArMeasurementMessage): String = stringResource(
    when (message) {
        ArMeasurementMessage.STARTING -> R.string.ar_msg_starting
        ArMeasurementMessage.MOVE_PHONE -> R.string.ar_msg_move_phone
        ArMeasurementMessage.AIM_AND_CAPTURE_FIRST -> R.string.ar_msg_first
        ArMeasurementMessage.AIM_AND_CAPTURE_SECOND -> R.string.ar_msg_second
        ArMeasurementMessage.MEASUREMENT_READY -> R.string.ar_msg_ready
        ArMeasurementMessage.NO_SURFACE -> R.string.ar_msg_no_surface
        ArMeasurementMessage.SESSION_ERROR -> R.string.ar_msg_error
    },
)

private fun formatMeters(value: Double): String =
    String.format(Locale.getDefault(), "%.3f", value)
