package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ConsoleType
import com.example.data.model.GameEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.N64Gold
import com.example.ui.theme.N64Red
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PS1Blue
import com.example.ui.theme.PS1Navy
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val games by viewModel.games.collectAsStateWithLifecycle()
    val selectedConsole by viewModel.selectedConsoleFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showOnlyFavorites by viewModel.showOnlyFavorites.collectAsStateWithLifecycle()
    val gamepadState by viewModel.gamepadState.collectAsStateWithLifecycle()

    var showSearchBar by remember { mutableStateOf(false) }

    // SAF Document Picker for ROM import (.iso, .bin, .cue, .pbp, .z64, .n64, etc.)
    val romPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "imported_game.bin"
            var fileSize = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }
            viewModel.importRomFromUri(uri, fileName, fileSize)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header Bar
            item {
                HeaderSection(
                    isGamepadConnected = gamepadState.isControllerConnected,
                    controllerName = gamepadState.controllerName,
                    onSearchToggle = { showSearchBar = !showSearchBar }
                )
            }

            // Search Bar (Expandable)
            if (showSearchBar) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Buscar jogo por título...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("search_game_input"),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpar")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Filter Chips
            item {
                FilterChipsRow(
                    selectedConsole = selectedConsole,
                    showOnlyFavorites = showOnlyFavorites,
                    onSelectConsole = { viewModel.setConsoleFilter(it) },
                    onToggleFavorites = { viewModel.toggleFavoritesFilter() }
                )
            }

            // Hero Featured Game Banner
            item {
                FeaturedHeroBanner(
                    games = games,
                    onPlay = { viewModel.playGame(it) }
                )
            }

            // Games Section Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Jogos Instalados (${games.size})",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedConsole != null) selectedConsole!!.displayName else "Todos os consoles",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }

            // Game Cards
            if (games.isEmpty()) {
                item {
                    EmptyGamesState(onImportClick = { romPickerLauncher.launch(arrayOf("*/*")) })
                }
            } else {
                items(games, key = { it.id }) { game ->
                    GameCard(
                        game = game,
                        onPlay = { viewModel.playGame(game) },
                        onToggleFavorite = { viewModel.toggleFavorite(game) },
                        onDelete = { viewModel.deleteGame(game) }
                    )
                }
            }
        }

        // Floating Action Button to Import ROM
        FloatingActionButton(
            onClick = {
                romPickerLauncher.launch(
                    arrayOf(
                        "application/octet-stream",
                        "application/x-iso9660-image",
                        "application/zip",
                        "*/*"
                    )
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .testTag("import_rom_fab"),
            containerColor = NeonCyan,
            contentColor = BackgroundDark,
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = "Importar ROM")
                Text("Importar ROM", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HeaderSection(
    isGamepadConnected: Boolean,
    controllerName: String,
    onSearchToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "RetroPlay 64 & PSX",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isGamepadConnected) Color(0xFF00E676) else TextMuted)
                )
                Text(
                    text = if (isGamepadConnected) "Controle: $controllerName" else "Nenhum controle Bluetooth",
                    color = if (isGamepadConnected) Color(0xFF00E676) else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        IconButton(
            onClick = onSearchToggle,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(SurfaceElevated)
        ) {
            Icon(Icons.Default.Search, contentDescription = "Buscar", tint = TextPrimary)
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedConsole: ConsoleType?,
    showOnlyFavorites: Boolean,
    onSelectConsole: (ConsoleType?) -> Unit,
    onToggleFavorites: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedConsole == null && !showOnlyFavorites,
                onClick = { onSelectConsole(null) },
                label = { Text("Todos") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonCyan,
                    selectedLabelColor = BackgroundDark,
                    containerColor = SurfaceDark,
                    labelColor = TextSecondary
                )
            )
        }
        item {
            FilterChip(
                selected = selectedConsole == ConsoleType.PS1,
                onClick = { onSelectConsole(if (selectedConsole == ConsoleType.PS1) null else ConsoleType.PS1) },
                label = { Text("PlayStation 1") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PS1Blue,
                    selectedLabelColor = Color.White,
                    containerColor = SurfaceDark,
                    labelColor = TextSecondary
                )
            )
        }
        item {
            FilterChip(
                selected = selectedConsole == ConsoleType.N64,
                onClick = { onSelectConsole(if (selectedConsole == ConsoleType.N64) null else ConsoleType.N64) },
                label = { Text("Nintendo 64") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = N64Gold,
                    selectedLabelColor = BackgroundDark,
                    containerColor = SurfaceDark,
                    labelColor = TextSecondary
                )
            )
        }
        item {
            FilterChip(
                selected = showOnlyFavorites,
                onClick = onToggleFavorites,
                leadingIcon = {
                    Icon(
                        if (showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text("Favoritos") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFF4081),
                    selectedLabelColor = Color.White,
                    containerColor = SurfaceDark,
                    labelColor = TextSecondary
                )
            )
        }
    }
}

@Composable
private fun FeaturedHeroBanner(
    games: List<GameEntity>,
    onPlay: (GameEntity) -> Unit
) {
    val featured = games.firstOrNull { it.isBuiltInDemo } ?: games.firstOrNull() ?: return
    val isPS1 = featured.consoleType == ConsoleType.PS1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clickable { onPlay(featured) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            // Background Artwork
            val drawableRes = if (isPS1) {
                com.example.R.drawable.ps1_game_hero_1790054759614
            } else {
                com.example.R.drawable.n64_game_hero_1790054771488
            }
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = featured.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Scrim Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC0C0E14), Color(0xFC0C0E14)),
                            startY = 60f
                        )
                    )
            )

            // Details
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isPS1) PS1Blue else N64Gold)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = featured.consoleType.badge,
                            color = if (isPS1) Color.White else BackgroundDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = "DESTAQUE • 60 FPS",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = featured.title,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = featured.description,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = { onPlay(featured) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPS1) PS1Blue else N64Gold,
                            contentColor = if (isPS1) Color.White else BackgroundDark
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Jogar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCard(
    game: GameEntity,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val isPS1 = game.consoleType == ConsoleType.PS1
    val playMinutes = game.playTimeSeconds / 60

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onPlay() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isPS1) PS1Navy else Color(0xFF3E2723))
            ) {
                val drawableRes = if (isPS1) {
                    com.example.R.drawable.ps1_game_hero_1790054759614
                } else {
                    com.example.R.drawable.n64_game_hero_1790054771488
                }
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = game.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Game Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isPS1) PS1Blue else N64Gold)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = game.consoleType.badge,
                            color = if (isPS1) Color.White else BackgroundDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = game.region,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = game.title,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (playMinutes > 0) "Jogado: ${playMinutes}m" else "Não jogado recentemente",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (game.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favoritar",
                        tint = if (game.isFavorite) Color(0xFFFF4081) else TextMuted
                    )
                }

                if (!game.isBuiltInDemo) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isPS1) PS1Blue else N64Gold)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Jogar",
                        tint = if (isPS1) Color.White else BackgroundDark
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGamesState(onImportClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.SportsEsports,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = TextMuted
        )
        Text(
            text = "Nenhum jogo encontrado",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Importe suas próprias ROMs de PS1 (.iso, .bin, .cue, .pbp) ou Nintendo 64 (.z64, .n64, .v64).",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Button(
            onClick = onImportClick,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = BackgroundDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Selecionar Arquivo ROM", fontWeight = FontWeight.Bold)
        }
    }
}
