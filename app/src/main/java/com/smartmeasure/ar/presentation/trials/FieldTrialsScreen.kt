package com.smartmeasure.ar.presentation.trials

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartmeasure.ar.R
import com.smartmeasure.ar.domain.model.CaptureCondition
import com.smartmeasure.ar.domain.model.FieldTrial
import com.smartmeasure.ar.domain.model.MeasurementKind
import com.smartmeasure.ar.domain.model.TrialSummary
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Below this many samples the percentile is effectively the maximum and must be read as such. */
private const val SMALL_SAMPLE = 10

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
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }

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
                    FilterChip(
                        selected = draft.kind == kind,
                        onClick = { onKindSelected(kind) },
                        label = { Text(kindText(kind)) },
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

            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.trials_save))
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
    val cm = stringResource(R.string.trials_unit_cm)
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
                    text = "${kindText(summary.kind)} · ${depthText(summary.depthEnabled)}",
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
                firstValue = "${centimeters(summary.meanAbsoluteErrorMeters)} $cm",
                secondLabel = stringResource(R.string.trials_stat_median),
                secondValue = "${centimeters(summary.medianAbsoluteErrorMeters)} $cm",
            )
            StatPair(
                firstLabel = stringResource(R.string.trials_stat_p90),
                firstValue = "${centimeters(summary.p90AbsoluteErrorMeters)} $cm",
                secondLabel = stringResource(R.string.trials_stat_max),
                secondValue = "${centimeters(summary.maxAbsoluteErrorMeters)} $cm",
            )
            StatPair(
                firstLabel = stringResource(R.string.trials_stat_bias),
                firstValue = "${signedCentimeters(summary.meanSignedErrorMeters)} $cm",
                secondLabel = stringResource(R.string.trials_stat_relative),
                secondValue = "${percent(summary.meanRelativeError)} %",
            )

            if (summary.count < SMALL_SAMPLE) {
                Text(
                    text = pluralStringResource(R.plurals.trials_small_sample, SMALL_SAMPLE, SMALL_SAMPLE),
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
    val recordedAt = recordedAtText(trial.recordedAtEpochMillis)
    val kind = kindText(trial.kind)
    val conditions = trial.conditions
        .sortedBy { it.ordinal }
        .map { conditionText(it) }
    val deleteDescription = stringResource(R.string.trials_delete_a11y, kind, recordedAt)

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
                    text = "$kind · ${depthText(trial.depthEnabled)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(
                        R.string.trials_row_error,
                        signedCentimeters(trial.signedErrorMeters),
                        percent(trial.relativeError),
                    ),
                    style = MaterialTheme.typography.titleMedium.tabular(),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(
                        R.string.trials_row_values,
                        meters(trial.arMeters),
                        meters(trial.referenceMeters),
                    ),
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                )
                Text(
                    text = (listOf(recordedAt) + conditions).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

@Composable
private fun DeleteTrialDialog(
    trial: FieldTrial,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trials_delete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = listOf(
                        kindText(trial.kind),
                        depthText(trial.depthEnabled),
                        recordedAtText(trial.recordedAtEpochMillis),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = stringResource(
                        R.string.trials_row_values,
                        meters(trial.arMeters),
                        meters(trial.referenceMeters),
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

@Composable
private fun recordedAtText(epochMillis: Long): String = remember(epochMillis) {
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
        .format(Date(epochMillis))
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

private fun meters(value: Double): String =
    String.format(Locale.getDefault(), "%.3f", value)

private fun centimeters(valueMeters: Double): String =
    String.format(Locale.getDefault(), "%.1f", valueMeters * 100)

private fun signedCentimeters(valueMeters: Double): String =
    String.format(Locale.getDefault(), "%+.1f", valueMeters * 100)

private fun percent(fraction: Double): String =
    String.format(Locale.getDefault(), "%.1f", fraction * 100)
