package com.reasontouch.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reasontouch.core.ui.theme.ReasonTouchTheme

private val KEY_ROOTS  = listOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
private val TIME_SIGS  = listOf(2, 3, 4, 5, 6, 7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSettingsScreen(
    onBack: () -> Unit,
    onExportMidi: () -> Unit,
    viewModel: SessionSettingsViewModel = hiltViewModel()
){
    val session      by viewModel.session.collectAsState()
    val focusManager = LocalFocusManager.current
    val skin         = ReasonTouchTheme.skin

    var nameText by remember(session?.name) {
        mutableStateOf(session?.name ?: "")
    }
    var keyDropdownExpanded by remember { mutableStateOf(false) }
    var transposeDelta by remember { mutableStateOf(0) }
    val transposeDisplay = if (transposeDelta > 0) "+$transposeDelta" else "$transposeDelta"

    val importStatus by viewModel.importStatus.collectAsState()
    if (importStatus != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.clearImportStatus() },
            title = { Text("Sound Pack Import", color = skin.textPrimary, fontFamily = FontFamily.Monospace) },
            text = { Text(importStatus!!, color = skin.textSecondary, fontFamily = FontFamily.Monospace) },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(skin.accent)
                        .clickable { viewModel.clearImportStatus() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = skin.panel,
            shape = RoundedCornerShape(skin.cornerRadiusMedium)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(skin.background)) {
        Column(modifier = Modifier.fillMaxSize().padding(bottom = 60.dp)) {

            // ── Minimalist Top Title Header ───────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth().height(44.dp)
                    .background(skin.rack)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "SESSION SETTINGS",
                    color = skin.accent, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace, letterSpacing = 2.sp
                )
            }
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(skin.accent))

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // ── Session name ───────────────────────────────────────────────
                item {
                    SettingsCard("SESSION") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Name", color = skin.textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            BasicTextField(
                                value = nameText,
                                onValueChange = { nameText = it },
                                textStyle = TextStyle(
                                    color = skin.accent, fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                cursorBrush = SolidColor(skin.accent),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    viewModel.setName(nameText)
                                    focusManager.clearFocus()
                                }),
                                modifier = Modifier.width(200.dp)
                            )
                        }
                    }
                }

                // ── Sound Pack (Future-Proofing .rpack System) ────────────────
                item {
                    val installedPacks by viewModel.installedPacks.collectAsState()
                    var packDropdownExpanded by remember { mutableStateOf(false) }
                    val currentPack = installedPacks.firstOrNull { it.packId == session?.packId } ?: installedPacks.firstOrNull()

                    SettingsCard("SOUND PACK") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Engine / Pack", color = skin.textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                ExposedDropdownMenuBox(
                                    expanded = packDropdownExpanded,
                                    onExpandedChange = { packDropdownExpanded = it }
                                ) {
                                    TextField(
                                        value = currentPack?.packName ?: "Default Sound Bank",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = packDropdownExpanded)
                                        },
                                        colors = TextFieldDefaults.colors(
                                            unfocusedContainerColor = skin.panelAlt,
                                            focusedContainerColor   = skin.panelAlt,
                                            unfocusedTextColor      = skin.accent,
                                            focusedTextColor        = skin.accent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedIndicatorColor   = Color.Transparent
                                        ),
                                        textStyle = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .width(180.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = packDropdownExpanded,
                                        onDismissRequest = { packDropdownExpanded = false },
                                        modifier = Modifier.background(skin.panel)
                                    ) {
                                        installedPacks.forEach { pack ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(pack.packName,
                                                        color = if (pack.packId == (session?.packId ?: "default")) skin.accent else skin.textPrimary,
                                                        fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                },
                                                onClick = {
                                                    viewModel.setPackId(pack.packId)
                                                    packDropdownExpanded = false
                                                },
                                                modifier = Modifier.background(if (pack.packId == (session?.packId ?: "default")) skin.accent.copy(alpha = 0.15f) else Color.Transparent)
                                            )
                                        }
                                    }
                                }
                            }

                            val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                                contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
                            ) { uri ->
                                uri?.let { viewModel.importPack(it) }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(skin.panelAlt)
                                    .border(1.dp, skin.border, RoundedCornerShape(4.dp))
                                    .clickable { launcher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*")) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "+ IMPORT .RPACK / .ZIP PACK",
                                    color = skin.accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (session?.packId != null && session?.packId != "default") {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(skin.accent.copy(alpha = 0.2f))
                                        .border(1.dp, skin.accent, RoundedCornerShape(4.dp))
                                        .clickable { viewModel.deletePack(session!!.packId) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "🗑 DELETE CURRENT PACK",
                                        color = skin.accent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Tempo ──────────────────────────────────────────────────────
                item {
                    SettingsCard("TEMPO") {
                        SettingsStepperRow(
                            label           = "BPM",
                            value           = "${session?.bpm ?: 120}",
                            onDecrement     = { viewModel.setBpm((session?.bpm ?: 120) - 1) },
                            onIncrement     = { viewModel.setBpm((session?.bpm ?: 120) + 1) },
                            onFastDecrement = { viewModel.setBpm((session?.bpm ?: 120) - 5) },
                            onFastIncrement = { viewModel.setBpm((session?.bpm ?: 120) + 5) }
                        )
                    }
                }

                // ── Master Transpose ───────────────────────────────────────────
                item {
                    SettingsCard("MASTER TRANSPOSE") {
                        SettingsStepperRow(
                            label           = "Semitones",
                            value           = transposeDisplay,
                            onDecrement     = { transposeDelta -= 1; viewModel.transposeSession(-1) },
                            onIncrement     = { transposeDelta += 1; viewModel.transposeSession(1) },
                            onFastDecrement = { transposeDelta -= 12; viewModel.transposeSession(-12) },
                            onFastIncrement = { transposeDelta += 12; viewModel.transposeSession(12) }
                        )
                    }
                }

                // ── Structure ──────────────────────────────────────────────────
                item {
                    SettingsCard("STRUCTURE") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Beats / Bar", color = skin.textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TIME_SIGS.forEach { num ->
                                    val isSelected = session?.timeSignatureNumerator == num
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) skin.accent.copy(alpha = 0.2f) else skin.panelAlt)
                                            .border(1.dp, if (isSelected) skin.accent else skin.border, RoundedCornerShape(4.dp))
                                            .clickable { viewModel.setTimeSignatureNumerator(num) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("$num/4",
                                            color = if (isSelected) skin.accent else skin.textMuted,
                                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Key ────────────────────────────────────────────────────────
                item {
                    SettingsCard("KEY") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Root", color = skin.textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                ExposedDropdownMenuBox(
                                    expanded = keyDropdownExpanded,
                                    onExpandedChange = { keyDropdownExpanded = it }
                                ) {
                                    TextField(
                                        value = session?.keyRoot ?: "C",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = keyDropdownExpanded)
                                        },
                                        colors = TextFieldDefaults.colors(
                                            unfocusedContainerColor = skin.panelAlt,
                                            focusedContainerColor   = skin.panelAlt,
                                            unfocusedTextColor      = skin.accent,
                                            focusedTextColor        = skin.accent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedIndicatorColor   = Color.Transparent
                                        ),
                                        textStyle = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize   = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .width(120.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = keyDropdownExpanded,
                                        onDismissRequest = { keyDropdownExpanded = false },
                                        modifier = Modifier.background(skin.panel)
                                    ) {
                                        KEY_ROOTS.forEach { root ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(root,
                                                        color = if (root == session?.keyRoot) skin.accent else skin.textPrimary,
                                                        fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                },
                                                onClick = {
                                                    viewModel.setKeyRoot(root)
                                                    keyDropdownExpanded = false
                                                },
                                                modifier = Modifier.background(if (root == session?.keyRoot) skin.accent.copy(alpha = 0.15f) else Color.Transparent)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Mode", color = skin.textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("MAJOR" to "Major (Bright)", "MINOR" to "Minor (Dark)")
                                        .forEach { (quality, label) ->
                                            val isSelected = session?.keyQuality == quality
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isSelected) skin.accent.copy(alpha = 0.2f) else skin.panelAlt)
                                                    .border(1.dp, if (isSelected) skin.accent else skin.border, RoundedCornerShape(4.dp))
                                                    .clickable { viewModel.setKeyQuality(quality) }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text(label,
                                                    color = if (isSelected) skin.accent else skin.textMuted,
                                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }

        // ── Bottom Action Bar (< BACK & ↓ MIDI) ─────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(skin.rack)
                .align(Alignment.BottomCenter)
                .border(1.dp, skin.border, RoundedCornerShape(0.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(skin.cornerRadiusSmall))
                    .background(skin.panel)
                    .border(1.dp, skin.border, RoundedCornerShape(skin.cornerRadiusSmall))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("< BACK", color = skin.textPrimary, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(skin.cornerRadiusSmall))
                    .background(skin.success.copy(alpha = 0.2f))
                    .border(1.dp, skin.success, RoundedCornerShape(skin.cornerRadiusSmall))
                    .clickable(onClick = onExportMidi)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "↓ MIDI EXPORT",
                    color = skin.success,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ── Shared components ─────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    val skin = ReasonTouchTheme.skin
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(skin.cornerRadiusMedium))
            .background(skin.panel)
            .border(1.dp, skin.border, RoundedCornerShape(skin.cornerRadiusMedium))
            .padding(14.dp)
    ) {
        Text(
            text = title,
            color = skin.accent, fontSize = 11.sp,
            fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        content()
    }
}

@Composable
private fun SettingsStepperRow(
    label:           String,
    value:           String,
    onDecrement:     () -> Unit,
    onIncrement:     () -> Unit,
    onFastDecrement: () -> Unit = {},
    onFastIncrement: () -> Unit = {}
) {
    val skin = ReasonTouchTheme.skin
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = skin.textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StepBtn("<<") { onFastDecrement() }
            StepBtn("<")  { onDecrement() }
            Text(
                value,
                color = skin.accentSecondary, fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(56.dp),
                textAlign = TextAlign.Center
            )
            StepBtn(">")  { onIncrement() }
            StepBtn(">>") { onFastIncrement() }
        }
    }
}

@Composable
private fun StepBtn(label: String, onClick: () -> Unit) {
    val skin = ReasonTouchTheme.skin
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(skin.panelAlt)
            .border(1.dp, skin.border, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = skin.textPrimary, fontSize = 12.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
