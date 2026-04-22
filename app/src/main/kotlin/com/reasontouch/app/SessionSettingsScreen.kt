package com.reasontouch.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private val BG       = Color(0xFF1A1A1E)
private val RACK     = Color(0xFF222228)
private val PANEL    = Color(0xFF2A2A32)
private val BORDER   = Color(0xFF3A3A45)
private val ACCENT   = Color(0xFFE84040)
private val ACCENT2  = Color(0xFFFF6B35)
private val GREEN    = Color(0xFF3DDC84)
private val TEXT     = Color(0xFFC8C8D4)
private val TEXT_DIM = Color(0xFF666675)
private val BLUE     = Color(0xFF38BDF8)
private val GOLD     = Color(0xFFF5C518)
private val PURPLE   = Color(0xFFA78BFA)

private val KEY_ROOTS  = listOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
private val TIME_SIGS  = listOf(2, 3, 4, 5, 6, 7)
private val BAR_OPTIONS = listOf(1, 2, 4, 8, 16, 32, 64)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSettingsScreen(
    onBack: () -> Unit,
    viewModel: SessionSettingsViewModel = hiltViewModel()
) {
    val session      by viewModel.session.collectAsState()
    val focusManager = LocalFocusManager.current

    var nameText by remember(session?.name) {
        mutableStateOf(session?.name ?: "")
    }
    var keyDropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(BG)) {

        // ── Toolbar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth().height(48.dp)
                .background(RACK)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(PANEL)
                    .border(1.dp, BORDER, RoundedCornerShape(4.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("< BACK", color = TEXT_DIM, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Text(
                "SESSION SETTINGS",
                color = ACCENT, fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp
            )
            Box(modifier = Modifier.width(72.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(ACCENT))

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // ── Session name ───────────────────────────────────────────────
            item {
                SettingsSection("SESSION")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(RACK)
                        .border(1.dp, BORDER, RoundedCornerShape(4.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Name", color = TEXT, fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace)
                    BasicTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        textStyle = TextStyle(
                            color = ACCENT2, fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(ACCENT2),
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

            // ── Tempo ──────────────────────────────────────────────────────
            item {
                SettingsSection("TEMPO")
                SettingsStepperRow(
                    label           = "BPM",
                    value           = "${session?.bpm ?: 120}",
                    onDecrement     = { viewModel.setBpm((session?.bpm ?: 120) - 1) },
                    onIncrement     = { viewModel.setBpm((session?.bpm ?: 120) + 1) },
                    onFastDecrement = { viewModel.setBpm((session?.bpm ?: 120) - 5) },
                    onFastIncrement = { viewModel.setBpm((session?.bpm ?: 120) + 5) }
                )
            }

            // ── Structure ──────────────────────────────────────────────────
            item {
                SettingsSection("STRUCTURE")
                SettingsRow(label = "Total Bars") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BAR_OPTIONS.forEach { bars ->
                            val isSelected = session?.totalBars == bars
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) ACCENT else PANEL)
                                    .border(1.dp,
                                        if (isSelected) ACCENT else BORDER,
                                        RoundedCornerShape(4.dp))
                                    .clickable { viewModel.setTotalBars(bars) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("$bars",
                                    color = if (isSelected) Color.White else TEXT_DIM,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
                SettingsRow(label = "Beats / Bar") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TIME_SIGS.forEach { num ->
                            val isSelected = session?.timeSignatureNumerator == num
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isSelected) Color(0xFF1A3A5A) else PANEL)
                                    .border(1.dp,
                                        if (isSelected) BLUE else BORDER,
                                        RoundedCornerShape(4.dp))
                                    .clickable {
                                        viewModel.setTimeSignatureNumerator(num)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("$num/4",
                                    color = if (isSelected) BLUE else TEXT_DIM,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            // ── Key ────────────────────────────────────────────────────────
            item {
                SettingsSection("KEY")

                // Key root — dropdown
                SettingsRow(label = "Root") {
                    ExposedDropdownMenuBox(
                        expanded = keyDropdownExpanded,
                        onExpandedChange = { keyDropdownExpanded = it }
                    ) {
                        TextField(
                            value = session?.keyRoot ?: "C",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = keyDropdownExpanded)
                            },
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = PANEL,
                                focusedContainerColor   = PANEL,
                                unfocusedTextColor      = GREEN,
                                focusedTextColor        = GREEN,
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
                            modifier = Modifier.background(PANEL)
                        ) {
                            KEY_ROOTS.forEach { root ->
                                DropdownMenuItem(
                                    text = {
                                        Text(root,
                                            color = if (root == session?.keyRoot)
                                                GREEN else TEXT,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace)
                                    },
                                    onClick = {
                                        viewModel.setKeyRoot(root)
                                        keyDropdownExpanded = false
                                    },
                                    modifier = Modifier.background(
                                        if (root == session?.keyRoot)
                                            Color(0xFF1A2A1A) else Color.Transparent)
                                )
                            }
                        }
                    }
                }

                // Key quality — Major / Minor chips
                SettingsRow(label = "Mode") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("MAJOR" to "Major (Bright)", "MINOR" to "Minor (Dark)")
                            .forEach { (quality, label) ->
                                val isSelected = session?.keyQuality == quality
                                val col = if (quality == "MAJOR") GOLD else PURPLE
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isSelected) col.copy(alpha = 0.15f)
                                            else PANEL)
                                        .border(1.dp,
                                            if (isSelected) col else BORDER,
                                            RoundedCornerShape(4.dp))
                                        .clickable { viewModel.setKeyQuality(quality) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(label,
                                        color = if (isSelected) col else TEXT_DIM,
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace)
                                }
                            }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// ── Shared components ─────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        color = TEXT_DIM, fontSize = 10.sp,
        fontFamily = FontFamily.Monospace, letterSpacing = 2.sp,
        modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(RACK)
            .border(1.dp, BORDER, RoundedCornerShape(4.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TEXT, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(RACK)
            .border(1.dp, BORDER, RoundedCornerShape(4.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TEXT, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StepBtn("<<") { onFastDecrement() }
            StepBtn("<")  { onDecrement() }
            Text(
                value,
                color = ACCENT2, fontSize = 16.sp,
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
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(PANEL)
            .border(1.dp, BORDER, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TEXT, fontSize = 12.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}