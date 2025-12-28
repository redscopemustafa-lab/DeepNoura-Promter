package com.bungamust.deepnoura.ui.builder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bungamust.deepnoura.model.Segment
import kotlin.math.max

@Composable
fun SegmentEditorDialog(
    segment: Segment?,
    onDismiss: () -> Unit,
    onSave: (Segment) -> Unit
) {
    var title by remember(segment) { mutableStateOf(segment?.title.orEmpty()) }
    var durationText by remember(segment) { mutableStateOf(segment?.durationSec?.toString() ?: "60") }
    var body by remember(segment) { mutableStateOf(segment?.body.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (segment == null) "Add Segment" else "Edit Segment") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { value ->
                        durationText = value.filter { it.isDigit() }.take(5).ifBlank { "0" }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Duration (seconds)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Body") },
                    minLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        },
        confirmButton = {
            Row {
                Button(onClick = {
                    val duration = max(1, durationText.toIntOrNull() ?: 60)
                    onSave(
                        (segment ?: Segment(body = body, durationSec = duration))
                            .copy(title = title.ifBlank { null }, body = body, durationSec = duration)
                    )
                }) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
