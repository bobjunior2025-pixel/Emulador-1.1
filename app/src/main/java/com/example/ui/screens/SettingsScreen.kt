package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AspectRatioMode
import com.example.data.model.ResolutionScale
import com.example.emulator.libretro.LibretroCoreManager
import com.example.ui.MainViewModel
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.emulatorSettings.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Configurações do Emulador",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
            )
            Text(
                text = "Ajustes de renderização gráfica, áudio e hardware dos núcleos PS1 e N64.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // Video / Display Section
        item {
            SectionHeader(icon = Icons.Default.Tv, title = "Vídeo e Renderização")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // CRT Scanlines
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Filtro CRT Scanlines", color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 14.sp)
                            Text("Simula linhas de varredura de TV de tubo clássica.", color = TextMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.scanlinesEnabled,
                            onCheckedChange = { viewModel.updateSettings { curr -> curr.copy(scanlinesEnabled = it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = NeonCyan,
                                uncheckedTrackColor = SurfaceElevated
                            )
                        )
                    }

                    if (settings.scanlinesEnabled) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Intensidade das Linhas CRT", color = TextSecondary, fontSize = 12.sp)
                                Text("${(settings.scanlineIntensity * 100).toInt()}%", color = NeonCyan, fontSize = 12.sp)
                            }
                            Slider(
                                value = settings.scanlineIntensity,
                                onValueChange = { viewModel.updateSettings { curr -> curr.copy(scanlineIntensity = it) } },
                                valueRange = 0.1f..0.8f,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonCyan,
                                    activeTrackColor = NeonCyan,
                                    inactiveTrackColor = SurfaceElevated
                                )
                            )
                        }
                    }

                    // PS1 Dithering Simulation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pontilhamento Dithering PS1", color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 14.sp)
                            Text("Emula a matriz de dithering de 24 bits para 15 bits do PSX original.", color = TextMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.ps1Dithering,
                            onCheckedChange = { viewModel.updateSettings { curr -> curr.copy(ps1Dithering = it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = NeonCyan,
                                uncheckedTrackColor = SurfaceElevated
                            )
                        )
                    }
                }
            }
        }

        // Audio Section
        item {
            SectionHeader(icon = Icons.Default.VolumeUp, title = "Áudio e Som Sintetizado")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Audio Synthesis
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Síntese de Áudio em Tempo Real", color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 14.sp)
                            Text("Gera som PCM sintetizado com baixa latência.", color = TextMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.audioEnabled,
                            onCheckedChange = { viewModel.updateSettings { curr -> curr.copy(audioEnabled = it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = NeonCyan,
                                uncheckedTrackColor = SurfaceElevated
                            )
                        )
                    }

                    // PS1 Startup Chime
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Chime de Inicialização do PS1", color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 14.sp)
                            Text("Toca o clássico som de boot ao iniciar títulos de PS1.", color = TextMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.playPs1StartupChime,
                            onCheckedChange = { viewModel.updateSettings { curr -> curr.copy(playPs1StartupChime = it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = NeonCyan,
                                uncheckedTrackColor = SurfaceElevated
                            )
                        )
                    }

                    // Volume Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Volume Geral", color = TextSecondary, fontSize = 12.sp)
                            Text("${(settings.audioVolume * 100).toInt()}%", color = NeonCyan, fontSize = 12.sp)
                        }
                        Slider(
                            value = settings.audioVolume,
                            onValueChange = { viewModel.updateSettings { curr -> curr.copy(audioVolume = it) } },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = SurfaceElevated
                            )
                        )
                    }
                }
            }
        }

        // Hardware Emulation Section
        item {
            SectionHeader(icon = Icons.Default.Memory, title = "Hardware Emulado")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // N64 Expansion Pak
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nintendo 64 Expansion Pak (8MB RAM)", color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 14.sp)
                            Text("Expande a memória de 4MB para 8MB para jogos exigentes.", color = TextMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.n64ExpansionPak,
                            onCheckedChange = { viewModel.updateSettings { curr -> curr.copy(n64ExpansionPak = it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = NeonCyan,
                                uncheckedTrackColor = SurfaceElevated
                            )
                        )
                    }
                }
            }
        }

        // Libretro Cores Management Section
        item {
            SectionHeader(icon = Icons.Default.Memory, title = "Núcleos Libretro (Emulação Real)")
        }

        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Integração Libretro (Mupen64Plus-Next & PCSX ReARMed)",
                        color = TextPrimary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "Permite acionar os núcleos Libretro instalados no Android para executar as instruções MIPS reais das ROMs comerciais importadas.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    LibretroCoreManager.AVAILABLE_CORES.forEach { core ->
                        val isInstalled = LibretroCoreManager.findInstalledPackage(context, core) != null
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceElevated)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(core.name, color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    if (isInstalled) "Instalado e Pronto" else "Toque para instalar o núcleo",
                                    color = if (isInstalled) Color(0xFF81C784) else NeonCyan,
                                    fontSize = 11.sp
                                )
                            }
                            if (!isInstalled) {
                                androidx.compose.material3.TextButton(
                                    onClick = { LibretroCoreManager.openStoreForCore(context, core) }
                                ) {
                                    Text("Instalar", color = NeonCyan, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // About Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Text("Sobre os Núcleos de Emulação", color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text("• PlayStation 1 Core: MIPS R3000A 33.8 MHz + GPU GTE 3D Rasterizer.", color = TextSecondary, fontSize = 12.sp)
                    Text("• Nintendo 64 Core: NEC VR4300 93.75 MHz + RCP 64-bit Reality Coprocessor.", color = TextSecondary, fontSize = 12.sp)
                    Text("• Controles Bluetooth: Android InputDevice HID nativo com polling de baixa latência.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
        Text(title, color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 15.sp)
    }
}
