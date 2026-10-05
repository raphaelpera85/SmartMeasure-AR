package com.smartmeasure.ar.presentation.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartmeasure.ar.R
import java.util.Locale

@Composable
fun ManualMeasurementScreen(
    uiState: ManualMeasurementUiState,
    onWidthChanged: (String) -> Unit,
    onLengthChanged: (String) -> Unit,
    onCalculate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.manual_measurement_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.manual_measurement_subtitle),
                style = MaterialTheme.typography.bodyLarge,
            )

            OutlinedTextField(
                value = uiState.widthInput,
                onValueChange = onWidthChanged,
                label = { Text(stringResource(R.string.width_meters)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.lengthInput,
                onValueChange = onLengthChanged,
                label = { Text(stringResource(R.string.length_meters)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            uiState.validationMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }

            uiState.geometry?.let { geometry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.manual_result_title), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(
                                R.string.area_result,
                                formatNumber(geometry.areaSquareMeters),
                            ),
                        )
                        Text(
                            stringResource(
                                R.string.perimeter_result,
                                formatNumber(geometry.perimeterMeters),
                            ),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onCalculate, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.calculate))
                }
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.back))
                }
            }

            Text(
                text = stringResource(R.string.manual_measurement_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatNumber(value: Double): String =
    String.format(Locale.getDefault(), "%.2f", value)
