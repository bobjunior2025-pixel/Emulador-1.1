package com.example.emulator.libretro

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.ConsoleType
import com.example.data.model.GameEntity
import java.io.File

/**
 * Libretro Core specification and configuration for retro consoles.
 */
data class LibretroCoreInfo(
    val id: String,
    val name: String,
    val consoleType: ConsoleType,
    val description: String,
    val recommendedAppPackage: String,
    val alternativePackages: List<String>,
    val supportedExtensions: List<String>,
    val coreFileName: String, // e.g., mupen64plus_next_libretro_android.so
    val playStoreUrl: String
)

/**
 * Manager responsible for Libretro core dispatch, detection of native RetroArch/standalone
 * cores, and launching ROMs through real Libretro-compliant native emulators.
 */
object LibretroCoreManager {

    val N64_CORE_MUPEN = LibretroCoreInfo(
        id = "mupen64plus_next",
        name = "Mupen64Plus-Next (Libretro)",
        consoleType = ConsoleType.N64,
        description = "Núcleo Libretro oficial de N64 com Dynarec MIPS R4300i e renderizador GLideN64/Angrylion.",
        recommendedAppPackage = "com.retroarch.aarch64",
        alternativePackages = listOf(
            "com.retroarch",
            "org.mupen64plusae.v3.fzurita", // Mupen64Plus FZ standalone
            "org.mupen64plusae.v3.fzurita.pro"
        ),
        supportedExtensions = listOf("z64", "n64", "v64", "zip"),
        coreFileName = "mupen64plus_next_libretro_android.so",
        playStoreUrl = "https://play.google.com/store/apps/details?id=com.retroarch"
    )

    val PS1_CORE_PCSX = LibretroCoreInfo(
        id = "pcsx_rearmed",
        name = "PCSX ReARMed (Libretro)",
        consoleType = ConsoleType.PS1,
        description = "Núcleo Libretro otimizado para ARM com Dynarec e suporte completo a CD-ROM e BIOS.",
        recommendedAppPackage = "com.retroarch.aarch64",
        alternativePackages = listOf(
            "com.retroarch",
            "com.github.stenzek.duckstation", // DuckStation standalone
            "com.epsxe.ePSXe"
        ),
        supportedExtensions = listOf("iso", "bin", "cue", "pbp", "chd", "img"),
        coreFileName = "pcsx_rearmed_libretro_android.so",
        playStoreUrl = "https://play.google.com/store/apps/details?id=com.retroarch"
    )

    val DUCKSTATION_CORE = LibretroCoreInfo(
        id = "duckstation",
        name = "DuckStation (Libretro / Standalone)",
        consoleType = ConsoleType.PS1,
        description = "Renderizador de alta fidelidade para PS1 com upscaling 4K e PGXP.",
        recommendedAppPackage = "com.github.stenzek.duckstation",
        alternativePackages = listOf("com.retroarch.aarch64", "com.retroarch"),
        supportedExtensions = listOf("iso", "bin", "cue", "chd", "pbp"),
        coreFileName = "duckstation_libretro_android.so",
        playStoreUrl = "https://play.google.com/store/apps/details?id=com.github.stenzek.duckstation"
    )

    val AVAILABLE_CORES = listOf(N64_CORE_MUPEN, PS1_CORE_PCSX, DUCKSTATION_CORE)

    fun getCoreForGame(game: GameEntity): LibretroCoreInfo {
        return when (game.consoleType) {
            ConsoleType.N64 -> N64_CORE_MUPEN
            ConsoleType.PS1 -> PS1_CORE_PCSX
        }
    }

    /**
     * Checks if a compatible native Libretro frontend (like RetroArch or standalone emulator)
     * is installed on the user device.
     */
    fun findInstalledPackage(context: Context, core: LibretroCoreInfo): String? {
        val pm = context.packageManager
        val candidates = listOf(core.recommendedAppPackage) + core.alternativePackages
        for (pkg in candidates) {
            try {
                pm.getPackageInfo(pkg, 0)
                return pkg
            } catch (_: PackageManager.NameNotFoundException) {
                // Not installed
            }
        }
        return null
    }

    /**
     * Returns true if any Libretro or dedicated core emulator is installed on the device.
     */
    fun isRealEmulatorAvailable(context: Context, game: GameEntity): Boolean {
        val core = getCoreForGame(game)
        return findInstalledPackage(context, core) != null
    }

    /**
     * Creates an Intent to launch the real native Libretro engine or standalone emulator with the ROM.
     */
    fun createRealLaunchIntent(context: Context, game: GameEntity): Intent? {
        val core = getCoreForGame(game)
        val installedPackage = findInstalledPackage(context, core)

        val romFile: File? = if (game.filePath.isNotBlank()) File(game.filePath) else null

        val romUri: Uri = if (romFile != null && romFile.exists()) {
            try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    romFile
                )
            } catch (_: Exception) {
                Uri.fromFile(romFile)
            }
        } else {
            return null
        }

        if (installedPackage != null) {
            // Target installed emulator with Libretro parameters
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(romUri, "*/*")
                setPackage(installedPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Libretro-specific intent extras (used by RetroArch Android)
                putExtra("LIBRETRO", core.coreFileName)
                putExtra("ROM", romFile.absolutePath)
                putExtra("CONFIGFILE", "")
            }
            return intent
        } else {
            // General VIEW intent for any installed ROM handler
            val generalIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(romUri, "application/octet-stream")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (generalIntent.resolveActivity(context.packageManager) != null) {
                return generalIntent
            }
        }

        return null
    }

    /**
     * Opens Google Play or browser to install the real Libretro Core / RetroArch runner.
     */
    fun openStoreForCore(context: Context, core: LibretroCoreInfo) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(core.playStoreUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(core.playStoreUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }
}
