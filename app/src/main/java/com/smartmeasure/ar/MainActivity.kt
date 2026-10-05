package com.smartmeasure.ar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartmeasure.ar.data.ar.ArCoreAvailabilityRepository
import com.smartmeasure.ar.data.ar.ArSessionInspector
import com.smartmeasure.ar.data.trial.FileFieldTrialRepository
import com.smartmeasure.ar.domain.model.ArPreparationResult
import com.smartmeasure.ar.domain.model.ArSessionSummary
import com.smartmeasure.ar.domain.model.DepthSupport
import com.smartmeasure.ar.presentation.ar.ArMeasureView
import com.smartmeasure.ar.presentation.ar.ArMeasurementScreen
import com.smartmeasure.ar.presentation.ar.ArMeasurementViewModel
import com.smartmeasure.ar.presentation.diagnostics.DiagnosticsScreen
import com.smartmeasure.ar.presentation.diagnostics.DiagnosticsViewModel
import com.smartmeasure.ar.presentation.manual.ManualMeasurementScreen
import com.smartmeasure.ar.presentation.manual.ManualMeasurementViewModel
import com.smartmeasure.ar.presentation.trials.FieldTrialsScreen
import com.smartmeasure.ar.presentation.trials.FieldTrialsViewModel
import com.smartmeasure.ar.ui.theme.SmartMeasureTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private val sessionInspector = ArSessionInspector()
    private val diagnosticsViewModel: DiagnosticsViewModel by viewModels {
        DiagnosticsViewModel.Factory(
            ArCoreAvailabilityRepository(applicationContext),
        )
    }
    private val manualMeasurementViewModel: ManualMeasurementViewModel by viewModels()
    private val arMeasurementViewModel: ArMeasurementViewModel by viewModels()
    private val fieldTrialsViewModel: FieldTrialsViewModel by viewModels {
        FieldTrialsViewModel.Factory(
            repository = FileFieldTrialRepository(File(filesDir, FIELD_TRIALS_FILE)),
            deviceModel = Build.MANUFACTURER + " " + Build.MODEL,
        )
    }
    private var destination by mutableStateOf(Destination.DIAGNOSTICS)
    private var fieldTrialsReturnDestination = Destination.DIAGNOSTICS

    private var installRequested = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            prepareAr(userRequestedInstall = !installRequested)
        } else {
            diagnosticsViewModel.onCameraPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartMeasureTheme {
                when (destination) {
                    Destination.DIAGNOSTICS -> {
                        val uiState = diagnosticsViewModel.uiState.collectAsStateWithLifecycle().value
                        DiagnosticsScreen(
                            uiState = uiState,
                            onRefresh = diagnosticsViewModel::refreshAvailability,
                            onPrepareAr = ::requestPrepareAr,
                            onOpenManualMode = { destination = Destination.MANUAL_MEASUREMENT },
                            onOpenFieldTrials = {
                                fieldTrialsViewModel.startDraft(
                                    arMeters = null,
                                    depthEnabled = uiState.depthSupport == DepthSupport.SUPPORTED,
                                )
                                openFieldTrials(from = Destination.DIAGNOSTICS)
                            },
                        )
                    }

                    Destination.MANUAL_MEASUREMENT -> {
                        val uiState = manualMeasurementViewModel.uiState
                            .collectAsStateWithLifecycle().value
                        ManualMeasurementScreen(
                            uiState = uiState,
                            onWidthChanged = manualMeasurementViewModel::onWidthChanged,
                            onLengthChanged = manualMeasurementViewModel::onLengthChanged,
                            onCalculate = manualMeasurementViewModel::calculateRectangle,
                            onBack = { destination = Destination.DIAGNOSTICS },
                        )
                    }

                    Destination.AR_MEASUREMENT -> {
                        val uiState = arMeasurementViewModel.uiState
                            .collectAsStateWithLifecycle().value
                        ArMeasurementScreen(
                            uiState = uiState,
                            listener = object : ArMeasureView.Listener {
                                override fun onSessionReady(depthEnabled: Boolean) {
                                    arMeasurementViewModel.onSessionReady(depthEnabled)
                                }

                                override fun onTrackingChanged(tracking: Boolean) {
                                    arMeasurementViewModel.onTrackingChanged(tracking)
                                }

                                override fun onPointCaptured(
                                    pointCount: Int,
                                    distanceMeters: Double?,
                                ) {
                                    arMeasurementViewModel.onPointCaptured(
                                        pointCount,
                                        distanceMeters,
                                    )
                                }

                                override fun onNoSurface() {
                                    arMeasurementViewModel.onNoSurface()
                                }

                                override fun onSessionError() {
                                    arMeasurementViewModel.onSessionError()
                                }

                                override fun onSessionSummary(summary: ArSessionSummary) {
                                    arMeasurementViewModel.onSessionSummary(summary)
                                }
                            },
                            onResetState = arMeasurementViewModel::onReset,
                            onRecordTrial = {
                                fieldTrialsViewModel.startDraft(
                                    arMeters = uiState.distanceMeters,
                                    depthEnabled = uiState.depthEnabled,
                                )
                                // Leaving disposes the AR view and its anchors; the value now lives in the draft.
                                arMeasurementViewModel.onReset()
                                openFieldTrials(from = Destination.AR_MEASUREMENT)
                            },
                            onBack = { destination = Destination.DIAGNOSTICS },
                        )
                    }

                    Destination.FIELD_TRIALS -> {
                        val uiState = fieldTrialsViewModel.uiState
                            .collectAsStateWithLifecycle().value
                        FieldTrialsScreen(
                            uiState = uiState,
                            onArChanged = fieldTrialsViewModel::onArChanged,
                            onReferenceChanged = fieldTrialsViewModel::onReferenceChanged,
                            onKindSelected = fieldTrialsViewModel::onKindSelected,
                            onDepthChanged = fieldTrialsViewModel::onDepthChanged,
                            onConditionToggled = fieldTrialsViewModel::onConditionToggled,
                            onSave = fieldTrialsViewModel::save,
                            onDelete = fieldTrialsViewModel::delete,
                            onExport = ::shareFieldTrialsCsv,
                            onBack = { destination = fieldTrialsReturnDestination },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        diagnosticsViewModel.refreshAvailability()

        if (installRequested && hasCameraPermission()) {
            prepareAr(userRequestedInstall = false)
        }
    }

    private fun requestPrepareAr() {
        if (hasCameraPermission()) {
            prepareAr(userRequestedInstall = !installRequested)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun prepareAr(userRequestedInstall: Boolean) {
        diagnosticsViewModel.onPreparationStarted()
        val result = sessionInspector.prepare(
            activity = this,
            userRequestedInstall = userRequestedInstall,
        )
        installRequested = result is ArPreparationResult.InstallRequested
        diagnosticsViewModel.onPreparationResult(result)
        if (result is ArPreparationResult.Ready) {
            destination = Destination.AR_MEASUREMENT
        }
    }

    private fun openFieldTrials(from: Destination) {
        fieldTrialsReturnDestination = from
        destination = Destination.FIELD_TRIALS
    }

    /** Hands the CSV text to an app the user picks; nothing is sent without that explicit choice. */
    private fun shareFieldTrialsCsv() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.trials_export_chooser))
            putExtra(Intent.EXTRA_TEXT, fieldTrialsViewModel.exportCsv())
        }
        startActivity(Intent.createChooser(send, getString(R.string.trials_export_chooser)))
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

    private enum class Destination {
        DIAGNOSTICS,
        MANUAL_MEASUREMENT,
        AR_MEASUREMENT,
        FIELD_TRIALS,
    }

    private companion object {
        const val FIELD_TRIALS_FILE = "field_trials.tsv"
    }
}
