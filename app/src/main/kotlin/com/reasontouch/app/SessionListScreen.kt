package com.reasontouch.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reasontouch.core.data.Session
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionListScreen(
    onSessionSelected: (String) -> Unit,
    viewModel: SessionListViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    var showNewSessionDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1E))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFF222228))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "REASON TOUCH",
                    color = Color(0xFFE84040),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 3.sp
                )
                Text(
                    text = "SESSIONS",
                    color = Color(0xFF666675),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFFE84040))
            )

            // Session list or empty state
            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NO SESSIONS YET",
                            color = Color(0xFF666675),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to create your first session",
                            color = Color(0xFF444455),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(sessions) { session ->
                        SwipeToDeleteSessionCard(
                            session = session,
                            onClick = { onSessionSelected(session.id) },
                            onDelete = { viewModel.deleteSession(session) }
                        )
                    }
                }
            }

            // New session button
            Button(
                onClick = { showNewSessionDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE84040)
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "+ NEW SESSION",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }
        }
    }

    // New session dialog
    if (showNewSessionDialog) {
        NewSessionDialog(
            onConfirm = { name ->
                showNewSessionDialog = false
                viewModel.createSession(name) { sessionId ->
                    onSessionSelected(sessionId)
                }
            },
            onDismiss = { showNewSessionDialog = false }
        )
    }

    // Delete confirmation dialog
    sessionToDelete?.let { session ->
        DeleteSessionDialog(
            sessionName = session.name,
            onConfirm = {
                viewModel.deleteSession(session)
                sessionToDelete = null
            },
            onDismiss = { sessionToDelete = null }
        )
    }
}

@Composable
fun SessionCard(
    session: Session,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(session.updatedAt))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(Color(0xFF222228), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colour accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(40.dp)
                .background(Color(0xFFE84040), RoundedCornerShape(2.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.name.uppercase(),
                color = Color(0xFFC8C8D4),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${session.bpm} BPM",
                    color = Color(0xFFFF6B35),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${session.keyRoot} ${session.keyQuality}",
                    color = Color(0xFF666675),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = dateString,
                    color = Color(0xFF444455),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Text(
            text = ">",
            color = Color(0xFF444455),
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun NewSessionDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF222228),
        titleContentColor = Color(0xFFE84040),
        textContentColor = Color(0xFFC8C8D4),
        title = {
            Text(
                text = "NEW SESSION",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "SESSION NAME",
                    color = Color(0xFF666675),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = {
                        Text(
                            text = "e.g. Track 1, Verse Idea...",
                            color = Color(0xFF444455),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE84040),
                        unfocusedBorderColor = Color(0xFF3A3A45),
                        focusedTextColor = Color(0xFFC8C8D4),
                        unfocusedTextColor = Color(0xFFC8C8D4),
                        cursorColor = Color(0xFFE84040)
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim())
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE84040)
                )
            ) {
                Text(
                    text = "CREATE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    color = Color(0xFF666675),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    )
}

@Composable
fun DeleteSessionDialog(
    sessionName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF222228),
        titleContentColor = Color(0xFFE84040),
        textContentColor = Color(0xFFC8C8D4),
        title = {
            Text(
                text = "DELETE SESSION",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        },
        text = {
            Text(
                text = "Delete \"${sessionName}\"? This cannot be undone.",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5A1A1A)
                )
            ) {
                Text(
                    text = "DELETE",
                    color = Color(0xFFE84040),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    color = Color(0xFF666675),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteSessionCard(
    session: com.reasontouch.core.data.Session,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            // Red delete background revealed on swipe
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF5A1A1A)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "DELETE",
                    color = Color(0xFFE84040),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
        }
    ) {
        SessionCard(
            session = session,
            onClick = onClick,
            onLongPress = {}
        )
    }
}