package com.reasontouch.feature.export

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

private val BG     = Color(0xFF222228)
private val BORDER = Color(0xFF3A3A45)
private val ACCENT = Color(0xFFE84040)
private val GREEN  = Color(0xFF3DDC84)
private val TEXT   = Color(0xFFC8C8D4)
private val DIM    = Color(0xFF666675)

@Composable
fun ExportDialog(
    sessionId: String,
    sessionName: String,
    onDismiss: () -> Unit,
    viewModel: ExportViewModel
) {
    val exportState by viewModel.exportState.collectAsState()
    var fileName by remember { mutableStateOf(sessionName) }

    LaunchedEffect(exportState) {
        if (exportState is ExportViewModel.ExportState.Success) {
            delay(2000)
            viewModel.resetState()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (exportState !is ExportViewModel.ExportState.Exporting) onDismiss()
        },
        containerColor  = BG,
        titleContentColor = GREEN,
        textContentColor  = TEXT,
        title = {
            Text(
                text = "EXPORT MIDI",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 14.sp
            )
        },
        text = {
            Column {
                when (val state = exportState) {
                    is ExportViewModel.ExportState.Idle -> {
                        Text(text = "FILE NAME", color = DIM, fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 6.dp))
                        OutlinedTextField(
                            value = fileName,
                            onValueChange = { fileName = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = GREEN,
                                unfocusedBorderColor = BORDER,
                                focusedTextColor     = TEXT,
                                unfocusedTextColor   = TEXT,
                                cursorColor          = GREEN
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Exports chord progression + all piano roll tracks to Downloads.",
                            color = DIM, fontSize = 11.sp, fontFamily = FontFamily.Monospace
                        )
                    }
                    is ExportViewModel.ExportState.Exporting -> {
                        Column(modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = GREEN)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "EXPORTING...", color = GREEN, fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                        }
                    }
                    is ExportViewModel.ExportState.Success -> {
                        Column(modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "OK", color = GREEN, fontSize = 24.sp,
                                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "SAVED TO DOWNLOADS", color = GREEN, fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = state.fileName, color = TEXT, fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace)
                        }
                    }
                    is ExportViewModel.ExportState.Error -> {
                        Text(text = "EXPORT FAILED", color = ACCENT, fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = state.message, color = DIM, fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = {
            if (exportState is ExportViewModel.ExportState.Idle ||
                exportState is ExportViewModel.ExportState.Error) {
                Button(
                    onClick = { viewModel.exportMidi(fileName) },
                    colors  = ButtonDefaults.buttonColors(containerColor = GREEN)
                ) {
                    Text(text = "EXPORT", color = Color(0xFF1A1A1E),
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (exportState !is ExportViewModel.ExportState.Exporting) {
                TextButton(onClick = onDismiss) {
                    Text(text = "CANCEL", color = DIM, fontFamily = FontFamily.Monospace)
                }
            }
        }
    )
}