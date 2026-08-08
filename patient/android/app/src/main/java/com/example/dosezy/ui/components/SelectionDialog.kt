// SelectionDialog.kt
package com.example.dosezy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectionDialog(
    title: String,
    options: List<Pair<String, T>>,
    currentSelection: T,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        tonalElevation = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { (optionName, optionValue) ->
                    androidx.compose.material3.ListItem(
                        headlineContent = {
                            Text(
                                text = optionName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        leadingContent = {
                            RadioButton(
                                selected = currentSelection == optionValue,
                                onClick = { onOptionSelected(optionValue) }
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(optionValue) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Close")
            }
        }
    )
}