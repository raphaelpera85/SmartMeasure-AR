package com.smartmeasure.ar.presentation.trials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartmeasure.ar.R
import com.smartmeasure.ar.domain.model.ArSessionSummary
import com.smartmeasure.ar.domain.model.CaptureCondition
import com.smartmeasure.ar.domain.model.FieldTrial
import com.smartmeasure.ar.domain.model.MeasurementKind
import com.smartmeasure.ar.domain.model.TrialStatistics
import com.smartmeasure.ar.domain.model.TrialSummary
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Tabular figures keep decimal points aligned when error values are scanned down a column. */
private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

@Composable
fun FieldTrialsScreen(
    uiState: FieldTrialsUiState,
    onArChanged: (String) -> Unit,
    onReferenceChanged: (String) -> Unit,
    onKindSelected: (MeasurementKind) -> Unit,
    onDepthChanged: (Boolean) -> Unit,
    onConditionToggled: (CaptureCondition) -> Unit,
    onSave: () -> Unit,
    onDelete: (String) -> Unit,
    onExport: () -> Unit,
    onBack: () -> Unit,
    onRecoverStorage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmRecovery by rememberSaveable { mutableStateOf(false) }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.trials_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.trials_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            // Above the form: it explains why saving is disabled before the user tries.
            if (uiState.storageUnreadable) {
                item {
                    StorageUnreadableCard(
                        recoveryFailed = uiState.recoveryFailed,
                        onRecover = { confirmRecovery = true },
                    )
                }
            }

            item {
                TrialForm(
                    uiState = uiState,
                    onArChanged = onArChanged,
                    onReferenceChanged = onReferenceChanged,
                    onKindSelected = onKindSelected,
                    onDepthChanged = onDepthChanged,
                    onConditionToggled = onConditionToggled,
                    onSave = onSave,
                )
            }

            item { SectionTitle(stringResource(R.string.trials_summary_title)) }

            when {
                uiState.loading -> item { LoadingRow() }

                uiState.summaries.isEmpty() -> item {
                    Text(
                        text = stringResource(R.string.trials_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> items(uiState.summaries, key = { "${it.kind}-${it.depthEnabled}" }) { summary ->
                    SummaryCard(summary)
                }
            }

            // The disclaimer sits next to the numbers it qualifies, not below a long list.
            item {
                Text(
                    text = stringResource(R.string.trials_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onExport,
                        enabled = uiState.trials.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.trials_export))
                    }
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.back))
                    }
                }
            }

            if (uiState.trials.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.trials_list_title, uiState.trials.size)) }
                items(uiState.trials, key = { it.id }) { trial ->
                    TrialRow(trial = trial, onDelete = { pendingDelete = trial.id })
                }
            }
        }
    }

    val trialToDelete = pendingDelete?.let { id -> uiState.trials.firstOrNull { it.id == id } }
    if (trialToDelete != null) {
        DeleteTrialDialog(
            trial = trialToDelete,
            onConfirm = {
                onDelete(trialToDelete.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    // Hidden once the file is readable again (recovery succeeded elsewhere or storage changed).
    if (confirmRecovery && uiState.storageUnreadable) {
        RecoverStorageDialog(
            onConfirm = {
                confirmRecovery = false
                onRecoverStorage()
            },
            onDismiss = { confirmRecovery = false },
        )
    }
}

/**
 * Storage could not be read: says what happened, that nothing is saved meanwhile, and offers the
 * one way out. The failure line appears inside the same card, next to the button that retries.
 */
@Composable
private fun StorageUnreadableCard(recoveryFailed: Boolean, onRecover: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.trials_storage_unreadable_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.trials_storage_unreadable_message),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
            if (recoveryFailed) {
                Text(
                    text = stringResource(R.string.trials_storage_recover_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                )
            }
            Button(
                onClick = onRecover,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.trials_storage_recover))
            }
        }
    }
}

@Composable
private fun RecoverStorageDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trials_storage_recover_title)) },
        text = { Text(stringResource(R.string.trials_storage_recover_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.trials_storage_recover_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .padding(top = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
        Text(
            text = stringResource(R.string.trials_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrialForm(
    uiState: FieldTrialsUiState,
    onArChanged: (String) -> Unit,
    onReferenceChanged: (String) -> Unit,
    onKindSelected: (MeasurementKind) -> Unit,
    onDepthChanged: (Boolean) -> Unit,
    onConditionToggled: (CaptureCondition) -> Unit,
    onSave: () -> Unit,
) {
    val draft = uiState.draft
    val arError = if (uiState.inputError == TrialInputError.INVALID_AR) {
        stringResource(R.string.trials_error_ar)
    } else {
        null
    }
    val referenceError = if (uiState.inputError == TrialInputError.INVALID_REFERENCE) {
        stringResource(R.string.trials_error_reference)
    } else {
        null
    }
    val inputStyle = MaterialTheme.typography.bodyLarge.tabular()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.trials_new_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )

            // Validation errors sit on the field they belong to and are announced as errors.
            OutlinedTextField(
                value = draft.arInput,
                onValueChange = onArChanged,
                label = { Text(stringResource(R.string.trials_ar_value)) },
                supportingText = arError?.let { message -> { Text(message) } },
                isError = arError != null,
                textStyle = inputStyle,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { if (arError != null) error(arError) },
            )
            OutlinedTextField(
                value = draft.referenceInput,
                onValueChange = onReferenceChanged,
                label = { Text(stringResource(R.string.trials_reference_value)) },
                supportingText = {
                    Text(referenceError ?: stringResource(R.string.trials_reference_hint))
                },
                isError = referenceError != null,
                textStyle = inputStyle,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { if (referenceError != null) error(referenceError) },
            )

            FieldLabel(stringResource(R.string.trials_kind_label))
            FlowRow(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MeasurementKind.entries.forEach { kind ->
                    // Exactly one kind applies: announce as a radio button, not FilterChip's checkbox.
                    FilterChip(
                        selected = draft.kind == kind,
                        onClick = { onKindSelected(kind) },
                        label = { Text(kindText(kind)) },
                        modifier = Modifier.semantics { role = Role.RadioButton },
                    )
                }
            }

            // The whole row toggles: the label is part of the 48dp target and is read with the state.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(
                        value = draft.depthEnabled,
                        role = Role.Switch,
                        onValueChange = onDepthChanged,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.trials_depth_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = stringResource(R.string.trials_depth_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = draft.depthEnabled, onCheckedChange = null)
            }

            FieldLabel(stringResource(R.string.trials_conditions_label))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CaptureCondition.entries.forEach { condition ->
                    FilterChip(
                        selected = condition in draft.conditions,
                        onClick = { onConditionToggled(condition) },
                        label = { Text(conditionText(condition)) },
                    )
                }
            }

            when {
                uiState.saveFailed -> StatusMessage(
                    text = stringResource(R.string.trials_error_storage),
                    isError = true,
                )

                uiState.justSaved -> StatusMessage(
                    text = stringResource(R.string.trials_saved),
                    isError = false,
                )
            }

            Button(
                onClick = onSave,
                enabled = !uiState.storageUnreadable,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.trials_save))
            }
            if (uiState.storageUnreadable) {
                Text(
                    text = stringResource(R.string.trials_save_blocked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** Save/storage feedback is announced by screen readers without moving focus. */
@Composable
private fun StatusMessage(text: String, isError: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics {
            liveRegion = if (isError) LiveRegionMode.Assertive else LiveRegionMode.Polite
        },
    )
}

@Composable
private fun SummaryCard(summary: TrialSummary) {
    val locale = currentLocale()
    fun cm(valueMeters: Double): String =
        TrialFormatting.centimeters(valueMeters, locale)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) { heading() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.trials_group_label,
                        kindText(summary.kind),
                        depthText(summary.depthEnabled),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = pluralStringResource(R.plurals.trials_count, summary.count, summary.count),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            StatPair(
                firstLabel = stringResource(R.string.trials_stat_mean),
                firstValue = stringResource(R.string.trials_value_cm, cm(summary.meanAbsoluteErrorMeters)),
                secondLabel = stringResource(R.string.trials_stat_median),
                secondValue = stringResource(R.string.trials_value_cm, cm(summary.medianAbsoluteErrorMeters)),
            )
            StatPair(
                firstLabel = stringResource(R.string.trials_stat_p90),
                firstValue = stringResource(R.string.trials_value_cm, cm(summary.p90AbsoluteErrorMeters)),
                secondLabel = stringResource(R.string.trials_stat_max),
                secondValue = stringResource(R.string.trials_value_cm, cm(summary.maxAbsoluteErrorMeters)),
            )
            StatPair(
                firstLabel = stringResource(R.string.trials_stat_bias),
                firstValue = stringResource(
                    R.string.trials_value_cm,
                    TrialFormatting.signedCentimeters(summary.meanSignedErrorMeters, locale),
                ),
                secondLabel = stringResource(R.string.trials_stat_relative),
                secondValue = stringResource(
                    R.string.trials_value_percent,
                    TrialFormatting.percent(summary.meanRelativeError, locale),
                ),
            )

            if (!summary.isRepresentative) {
                Text(
                    text = pluralStringResource(
                        R.plurals.trials_small_sample,
                        TrialStatistics.MIN_REPRESENTATIVE_SAMPLES,
                        TrialStatistics.MIN_REPRESENTATIVE_SAMPLES,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun StatPair(
    firstLabel: String,
    firstValue: String,
    secondLabel: String,
    secondValue: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatCell(firstLabel, firstValue, Modifier.weight(1f))
        StatCell(secondLabel, secondValue, Modifier.weight(1f))
    }
}

/** One labelled figure, merged so a screen reader reads "label, value" as a single stop. */
@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.tabular(),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TrialRow(trial: FieldTrial, onDelete: () -> Unit) {
    val locale = currentLocale()
    val recordedAt = recordedAtText(trial.recordedAtEpochMillis)
    val kind = kindText(trial.kind)
    val arText = TrialFormatting.meters(trial.arMeters, locale)
    val referenceText = TrialFormatting.meters(trial.referenceMeters, locale)
    val conditions = trial.conditions
        .sortedBy { it.ordinal }
        .map { conditionText(it) }
    val details = if (conditions.isEmpty()) {
        recordedAt
    } else {
        stringResource(
            R.string.trials_row_details,
            recordedAt,
            conditions.joinToString(stringResource(R.string.trials_conditions_separator)),
        )
    }
    // AR and reference values tell apart two trials of the same kind saved in the same minute.
    val deleteDescription =
        stringResource(R.string.trials_delete_a11y, kind, recordedAt, arText, referenceText)

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.trials_group_label, kind, depthText(trial.depthEnabled)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(
                        R.string.trials_row_error,
                        TrialFormatting.signedCentimeters(trial.signedErrorMeters, locale),
                        TrialFormatting.percent(trial.relativeError, locale),
                    ),
                    style = MaterialTheme.typography.titleMedium.tabular(),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.trials_row_values, arText, referenceText),
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                )
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                trial.session?.let { SessionSummaryLine(it) }
            }
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.semantics { contentDescription = deleteDescription },
            ) {
                Text(stringResource(R.string.trials_delete))
            }
        }
    }
}

/**
 * Secondary line with the AR session the trial came from. The visible text abbreviates planes
 * (H / V); screen readers get the spelled-out version.
 */
@Composable
private fun SessionSummaryLine(session: ArSessionSummary) {
    val locale = currentLocale()
    val distance = TrialFormatting.pathMeters(session.distanceMeters, locale)
    val losses = pluralStringResource(
        R.plurals.ar_session_losses,
        session.trackingLosses,
        session.trackingLosses,
    )
    val ratio = session.trackingRatio?.let { TrialFormatting.wholePercent(it, locale) }
    val text: String
    val description: String
    if (ratio != null) {
        text = stringResource(
            R.string.trials_row_session,
            distance, losses, ratio, session.horizontalPlanes, session.verticalPlanes,
        )
        description = stringResource(
            R.string.trials_row_session_a11y,
            distance, losses, ratio, session.horizontalPlanes, session.verticalPlanes,
        )
    } else {
        text = stringResource(
            R.string.trials_row_session_no_ratio,
            distance, losses, session.horizontalPlanes, session.verticalPlanes,
        )
        description = stringResource(
            R.string.trials_row_session_no_ratio_a11y,
            distance, losses, session.horizontalPlanes, session.verticalPlanes,
        )
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.tabular(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        // Replaces the semantic text (not contentDescription): the row merges its children, and a
        // description there would make TalkBack read it instead of the whole row.
        modifier = Modifier.clearAndSetSemantics { this.text = AnnotatedString(description) },
    )
}

@Composable
private fun DeleteTrialDialog(
    trial: FieldTrial,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = currentLocale()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trials_delete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(
                        R.string.trials_delete_summary,
                        kindText(trial.kind),
                        depthText(trial.depthEnabled),
                        recordedAtText(trial.recordedAtEpochMillis),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = stringResource(
                        R.string.trials_row_values,
                        TrialFormatting.meters(trial.arMeters, locale),
                        TrialFormatting.meters(trial.referenceMeters, locale),
                    ),
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                )
                Text(stringResource(R.string.trials_delete_message))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.trials_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/** Locale of the current configuration, so a language change recomposes formatted text. */
@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
private fun recordedAtText(epochMillis: Long): String {
    val locale = currentLocale()
    return remember(epochMillis, locale) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)
            .format(Date(epochMillis))
    }
}

@Composable
private fun kindText(kind: MeasurementKind): String = stringResource(
    when (kind) {
        MeasurementKind.WALL -> R.string.kind_wall
        MeasurementKind.OPENING -> R.string.kind_opening
        MeasurementKind.HEIGHT -> R.string.kind_height
        MeasurementKind.OTHER -> R.string.kind_other
    },
)

@Composable
private fun conditionText(condition: CaptureCondition): String = stringResource(
    when (condition) {
        CaptureCondition.LOW_LIGHT -> R.string.condition_low_light
        CaptureCondition.PLAIN_SURFACE -> R.string.condition_plain_surface
        CaptureCondition.REFLECTIVE_SURFACE -> R.string.condition_reflective
        CaptureCondition.OBSTRUCTED -> R.string.condition_obstructed
        CaptureCondition.SMALL_ROOM -> R.string.condition_small_room
    },
)

@Composable
private fun depthText(depthEnabled: Boolean): String = stringResource(
    if (depthEnabled) R.string.trials_with_depth else R.string.trials_without_depth,
)
