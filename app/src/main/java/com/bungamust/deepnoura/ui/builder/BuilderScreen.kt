package com.bungamust.deepnoura.ui.builder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bungamust.deepnoura.model.Segment
import com.bungamust.deepnoura.viewmodel.ProjectViewModel
import kotlin.math.max

@Composable
fun BuilderScreen(
    paddingValues: PaddingValues,
    viewModel: ProjectViewModel,
    onNavigateToPlayer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingSegment by remember { mutableStateOf<Segment?>(null) }
    var showDeleteId by remember { mutableStateOf<String?>(null) }
    val totalDuration = uiState.project.segments.sumOf { it.durationSec }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingSegment = Segment(body = "", durationSec = 60) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = { Text("Add segment") }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(inner)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total duration: ${formatDuration(totalDuration)}",
                    style = MaterialTheme.typography.titleLarge
                )
                Button(onClick = onNavigateToPlayer) {
                    Text("Open Player")
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(uiState.project.segments, key = { it.id }) { segment ->
                    SegmentCard(
                        segment = segment,
                        onEdit = { editingSegment = segment },
                        onDelete = { showDeleteId = segment.id },
                        onMoveUp = { viewModel.moveSegmentUp(segment.id) },
                        onMoveDown = { viewModel.moveSegmentDown(segment.id) }
                    )
                }
            }
        }
    }

    if (editingSegment != null) {
        SegmentEditorDialog(
            segment = editingSegment,
            onDismiss = { editingSegment = null },
            onSave = { updated ->
                viewModel.upsertSegment(updated.copy(updatedAt = System.currentTimeMillis()))
                editingSegment = null
            }
        )
    }

    if (showDeleteId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteId = null },
            title = { Text("Delete segment") },
            text = { Text("Are you sure you want to delete this segment?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteId?.let { viewModel.deleteSegment(it) }
                    showDeleteId = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SegmentCard(
    segment: Segment,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = segment.title.takeUnless { it.isNullOrBlank() } ?: "Untitled",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Duration: ${formatDuration(segment.durationSec)} · ${wordCount(segment.body)} words",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
                    }
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move down")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = segment.body,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun wordCount(text: String): Int = text.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(max(0, mins), max(0, secs))
}
