package com.bungamust.deepnoura.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bungamust.deepnoura.viewmodel.PlayerMode
import com.bungamust.deepnoura.viewmodel.ProjectViewModel
import kotlin.math.max

@Composable
fun PlayerScreen(
    paddingValues: PaddingValues,
    viewModel: ProjectViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val segments = uiState.project.segments
    val player = uiState.playerState
    val currentSegment = segments.getOrNull(player.currentIndex)

    val scrollState = rememberScrollState()
    val lines = remember(currentSegment?.id) {
        currentSegment?.body?.splitToTimedLines() ?: emptyList()
    }

    LaunchedEffect(player.lineIndex, player.mode, player.isPlaying) {
        if (player.mode == PlayerMode.Karaoke && player.lineIndex >= lines.size) {
            viewModel.nextSegment()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (currentSegment != null) {
                Text(
                    text = currentSegment.title ?: "Segment ${player.currentIndex + 1}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeToggle(mode = player.mode, onModeChange = viewModel::setMode)
                    AssistChips(player = player, viewModel = viewModel)
                }
                Spacer(modifier = Modifier.height(8.dp))
                when (player.mode) {
                    PlayerMode.Prompter -> PrompterView(
                        text = currentSegment.body,
                        durationSec = currentSegment.durationSec,
                        speedMultiplier = player.speedMultiplier,
                        fontScale = player.fontScale,
                        lineSpacing = player.lineSpacing,
                        mirror = player.mirror,
                        isPlaying = player.isPlaying,
                        scrollState = scrollState
                    )
                    PlayerMode.Karaoke -> KaraokeView(
                        lines = lines,
                        lineIndex = player.lineIndex,
                        onLineComplete = { viewModel.updateLineIndex(player.lineIndex + 1) },
                        durationSec = currentSegment.durationSec,
                        speedMultiplier = player.speedMultiplier,
                        fontScale = player.fontScale,
                        lineSpacing = player.lineSpacing,
                        mirror = player.mirror,
                        isPlaying = player.isPlaying
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Add segments to start playing")
                }
            }
        }

        PlayerControls(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            hasSegments = segments.isNotEmpty(),
            isPlaying = player.isPlaying,
            onPlayPause = viewModel::playPause,
            onRestart = viewModel::restartSegment,
            onNext = viewModel::nextSegment,
            onPrevious = viewModel::previousSegment,
            speed = player.speedMultiplier,
            onSpeedChange = viewModel::setSpeedMultiplier,
            fontScale = player.fontScale,
            onFontScaleChange = viewModel::setFontScale,
            lineSpacing = player.lineSpacing,
            onLineSpacingChange = viewModel::setLineSpacing
        )
    }
}

@Composable
private fun ModeToggle(mode: PlayerMode, onModeChange: (PlayerMode) -> Unit) {
    SegmentedButtonRow {
        SegmentedButton(
            selected = mode == PlayerMode.Prompter,
            onClick = { onModeChange(PlayerMode.Prompter) },
            label = { Text("Prompter") },
            icon = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) }
        )
        SegmentedButton(
            selected = mode == PlayerMode.Karaoke,
            onClick = { onModeChange(PlayerMode.Karaoke) },
            label = { Text("Karaoke") },
            icon = { Icon(Icons.Default.MusicNote, contentDescription = null) }
        )
    }
}

@Composable
private fun AssistChips(player: com.bungamust.deepnoura.viewmodel.PlayerState, viewModel: ProjectViewModel) {
    Row {
        AssistChip(
            onClick = { viewModel.toggleMirror(!player.mirror) },
            label = { Text("Mirror") },
            leadingIcon = { Icon(Icons.Default.FlipCameraAndroid, contentDescription = null) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (player.mirror) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        AssistChip(
            onClick = { viewModel.toggleDarkMode(!player.isDarkMode) },
            label = { Text("Dark") },
            leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (player.isDarkMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun PrompterView(
    text: String,
    durationSec: Int,
    speedMultiplier: Float,
    fontScale: Float,
    lineSpacing: Float,
    mirror: Boolean,
    isPlaying: Boolean,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val lines = text.splitToTimedLines()
    LaunchedEffect(isPlaying, text, durationSec, speedMultiplier) {
        if (isPlaying) {
            scrollState.animateScrollTo(
                scrollState.maxValue,
                tween(((durationSec / speedMultiplier) * 1000).toInt(), easing = LinearEasing)
            )
        } else {
            scrollState.stopScroll()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(if (mirror) -1f else 1f, 1f)
            .verticalScroll(scrollState)
            .padding(vertical = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            lines.forEach { line ->
                Text(
                    text = line,
                    fontSize = (20.sp * fontScale),
                    lineHeight = (24.sp * lineSpacing),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(400.dp))
        }
    }
}

@Composable
private fun KaraokeView(
    lines: List<String>,
    lineIndex: Int,
    onLineComplete: () -> Unit,
    durationSec: Int,
    speedMultiplier: Float,
    fontScale: Float,
    lineSpacing: Float,
    mirror: Boolean,
    isPlaying: Boolean
) {
    val perLine = if (lines.isNotEmpty()) durationSec.toFloat() / lines.size else durationSec.toFloat()
    val highlightAnim by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(
            durationMillis = (perLine * 1000 / speedMultiplier).toInt(),
            easing = LinearEasing
        ),
        finishedListener = { if (isPlaying) onLineComplete() },
        label = "karaoke"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .scale(if (mirror) -1f else 1f, 1f)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        lines.forEachIndexed { index, line ->
            val isActive = index == lineIndex
            Text(
                text = line,
                fontSize = (24.sp * fontScale),
                lineHeight = (28.sp * lineSpacing),
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f * max(1f, highlightAnim)) else Color.Transparent,
                        shape = MaterialTheme.shapes.medium
                    )
                    .fillMaxWidth(0.9f)
            )
        }
    }
}

@Composable
private fun PlayerControls(
    modifier: Modifier = Modifier,
    hasSegments: Boolean,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onRestart: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    lineSpacing: Float,
    onLineSpacingChange: (Float) -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (hasSegments) onPrevious() }, enabled = hasSegments) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                }
                IconButton(onClick = { if (hasSegments) onRestart() }, enabled = hasSegments) {
                    Icon(Icons.Default.Replay, contentDescription = "Restart")
                }
                IconButton(onClick = { if (hasSegments) onPlayPause() }, enabled = hasSegments) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause"
                    )
                }
                IconButton(onClick = { if (hasSegments) onNext() }, enabled = hasSegments) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            ControlSlider(
                title = "Speed ${"%.1f".format(speed)}x",
                value = speed,
                onValueChange = onSpeedChange,
                valueRange = 0.7f..1.3f,
                icon = Icons.Default.Speed
            )
            ControlSlider(
                title = "Font size",
                value = fontScale,
                onValueChange = onFontScaleChange,
                valueRange = 0.8f..1.5f,
                icon = Icons.Default.TextFields
            )
            ControlSlider(
                title = "Line spacing",
                value = lineSpacing,
                onValueChange = onLineSpacingChange,
                valueRange = 1f..2f,
                icon = Icons.Default.FormatLineSpacing
            )
        }
    }
}

@Composable
private fun ControlSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun String.splitToTimedLines(targetLength: Int = 65): List<String> {
    val words = this.split("\\s+".toRegex()).filter { it.isNotBlank() }
    if (words.isEmpty()) return emptyList()
    val lines = mutableListOf<String>()
    var current = StringBuilder()
    for (word in words) {
        if (current.length + word.length + 1 > targetLength) {
            lines.add(current.toString().trim())
            current = StringBuilder()
        }
        current.append(word).append(' ')
    }
    if (current.isNotEmpty()) lines.add(current.toString().trim())
    return lines.ifEmpty { listOf(this) }
}
