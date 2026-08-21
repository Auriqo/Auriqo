package com.auriqo.music.ui.screens.settings

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.auriqo.music.utils.debug.DebugLogTree
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogScreen(navController: NavController) {
    val debugLogTree = remember { DebugLogTree.getInstance() }
    val logs by debugLogTree?.logs?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    var minLogLevel by remember { mutableIntStateOf(Log.DEBUG) }
    var autoScroll by remember { mutableStateOf(true) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, minLogLevel) {
        logs.filter { it.level >= minLogLevel }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Logs") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterMenu = !showFilterMenu }) {
                        Icon(Icons.Rounded.FilterList, contentDescription = "Filter")
                    }
                    IconButton(onClick = { debugLogTree?.clear() }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Clear")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            AnimatedVisibility(visible = showFilterMenu) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Min level:", style = MaterialTheme.typography.labelMedium)
                    FilterChip(
                        selected = minLogLevel == Log.VERBOSE,
                        onClick = { minLogLevel = Log.VERBOSE },
                        label = { Text("V") }
                    )
                    FilterChip(
                        selected = minLogLevel == Log.DEBUG,
                        onClick = { minLogLevel = Log.DEBUG },
                        label = { Text("D") }
                    )
                    FilterChip(
                        selected = minLogLevel == Log.INFO,
                        onClick = { minLogLevel = Log.INFO },
                        label = { Text("I") }
                    )
                    FilterChip(
                        selected = minLogLevel == Log.WARN,
                        onClick = { minLogLevel = Log.WARN },
                        label = { Text("W") }
                    )
                    FilterChip(
                        selected = minLogLevel == Log.ERROR,
                        onClick = { minLogLevel = Log.ERROR },
                        label = { Text("E") }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredLogs.size} entries",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-scroll",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Switch(
                        checked = autoScroll,
                        onCheckedChange = { autoScroll = it }
                    )
                }
            }

            HorizontalDivider()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filteredLogs, key = { "${it.timestamp}-${it.message.hashCode()}" }) { entry ->
                    LogEntryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: DebugLogTree.LogEntry) {
    val levelColor = when (entry.level) {
        Log.VERBOSE -> Color.Gray
        Log.DEBUG -> Color(0xFF64B5F6)
        Log.INFO -> Color(0xFF81C784)
        Log.WARN -> Color(0xFFFFD54F)
        Log.ERROR -> Color(0xFFE57373)
        Log.ASSERT -> Color(0xFFBA68C8)
        else -> Color.White
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                levelColor.copy(alpha = 0.08f),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = entry.formattedTime,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = entry.levelStr,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = levelColor,
            modifier = Modifier.width(16.dp)
        )
        Text(
            text = entry.tag ?: "?",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(80.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.message,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (entry.throwable != null) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
            entry.throwable?.let { throwable ->
                Text(
                    text = throwable.stackTraceToString(),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
