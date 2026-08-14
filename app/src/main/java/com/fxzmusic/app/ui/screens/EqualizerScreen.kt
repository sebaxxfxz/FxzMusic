package com.fxzmusic.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxzmusic.app.R
import com.fxzmusic.app.data.EqBand
import com.fxzmusic.app.ui.components.FrequencyResponseCurve
import com.fxzmusic.app.ui.components.GlassCard
import com.fxzmusic.app.ui.components.PressScale
import com.fxzmusic.app.ui.components.scaleOnPress
import com.fxzmusic.app.viewmodel.EqualizerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen(
    equalizerViewModel: EqualizerViewModel,
    onBack: () -> Unit = {}
) {
    val profile = equalizerViewModel.currentProfile
    val isEnabled = equalizerViewModel.isEnabled
    val accent = MaterialTheme.colorScheme.primary

    BackHandler(enabled = true) {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 56.dp, bottom = 120.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = stringResource(R.string.equalizer_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = { equalizerViewModel.toggleEnabled() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = accent,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isEnabled) 1f else 0.4f),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.frequency_curve_title),
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
                FrequencyResponseCurve(
                    bands = profile.bands,
                    accent = accent,
                    onBandChange = { index, gainDb ->
                        equalizerViewModel.updateBand(index, gainDb)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.easy_presets),
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    equalizerViewModel.allProfiles.forEach { p ->
                        val isSelected = profile.id == p.id
                        val interaction = remember { MutableInteractionSource() }

                        Box(
                            modifier = Modifier
                                .scaleOnPress(interaction, PressScale.chip)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) accent else Color.White.copy(alpha = 0.08f))
                                .clickable(interactionSource = interaction, indication = null) {
                                    equalizerViewModel.selectProfile(p)
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                p.name,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.sliders_title),
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        profile.bands.forEach { band ->
                            val friendlyName = when {
                                band.label.contains("60") || band.label.contains("32") || band.label.contains("64") -> stringResource(R.string.band_bass)
                                band.label.contains("230") || band.label.contains("125") || band.label.contains("250") -> stringResource(R.string.band_low_mid)
                                band.label.contains("910") || band.label.contains("500") || band.label.contains("1K") || band.label.contains("1000") -> stringResource(R.string.band_mid)
                                band.label.contains("3.6") || band.label.contains("2K") || band.label.contains("4K") -> stringResource(R.string.band_high_mid)
                                band.label.contains("14") || band.label.contains("8K") || band.label.contains("16K") -> stringResource(R.string.band_treble)
                                else -> band.label
                            }

                            EasyBandSlider(
                                label = friendlyName,
                                gainDb = band.gainDb,
                                accent = accent,
                                onValueChange = { gainDb ->
                                    equalizerViewModel.updateBand(band.index, gainDb)
                                }
                            )
                        }
                    }
                }
            }

            if (profile.isCustom) {
                var showSaveDialog by remember { mutableStateOf(false) }
                var profileName by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showSaveDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.save_my_preset),
                                color = accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            .clickable { equalizerViewModel.deleteCustomProfile(profile) }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.delete_custom_preset),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showSaveDialog) {
                    AlertDialog(
                        onDismissRequest = { showSaveDialog = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = { Text(stringResource(R.string.save_equalizer_dialog_title), fontWeight = FontWeight.Bold) },
                        text = {
                            OutlinedTextField(
                                value = profileName,
                                onValueChange = { profileName = it },
                                placeholder = { Text(stringResource(R.string.preset_name_hint)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                equalizerViewModel.saveCustomProfile(profileName)
                                showSaveDialog = false
                                profileName = ""
                            }) {
                                Text(stringResource(R.string.save_button), color = accent, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSaveDialog = false }) {
                                Text(stringResource(R.string.cancel_button), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EasyBandSlider(
    label: String,
    gainDb: Float,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = String.format("%+.1f dB", gainDb),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (gainDb != 0f) accent else Color.White.copy(alpha = 0.5f)
            )
        }

        Slider(
            value = gainDb,
            onValueChange = onValueChange,
            valueRange = -12f..12f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            )
        )
    }
}
