package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ConsoleType
import com.example.data.model.GameEntity
import com.example.data.model.SaveStateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [GameEntity::class, SaveStateEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun saveStateDao(): SaveStateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "retro_emulator_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialGames(database.gameDao())
                    }
                }
            }
        }

        suspend fun populateInitialGames(gameDao: GameDao) {
            val defaultGames = listOf(
                GameEntity(
                    id = 1,
                    title = "Ridge Polygon 3D (Demo)",
                    consoleType = ConsoleType.PS1,
                    filePath = "builtin://ps1_ridge_polygon",
                    coverDrawableName = "ps1_game_hero_1790054759614",
                    fileSizeBytes = 478_000_000L,
                    lastPlayedTimestamp = System.currentTimeMillis() - 120_000,
                    playTimeSeconds = 1420,
                    isFavorite = true,
                    region = "USA",
                    isBuiltInDemo = true,
                    description = "Demonstração 3D de alta velocidade para PS1 com rasterizador de polígonos, áudio PCM sintetizado e física de drift."
                ),
                GameEntity(
                    id = 2,
                    title = "Super Polygon 64 (Demo)",
                    consoleType = ConsoleType.N64,
                    filePath = "builtin://n64_super_polygon",
                    coverDrawableName = "n64_game_hero_1790054771488",
                    fileSizeBytes = 16_777_216L,
                    lastPlayedTimestamp = System.currentTimeMillis() - 360_000,
                    playTimeSeconds = 2380,
                    isFavorite = true,
                    region = "USA",
                    isBuiltInDemo = true,
                    description = "Plataforma 3D aberta para Nintendo 64 com física analógica de 360 graus, coleta de estrelas, névoa volumétrica e iluminação Gouraud."
                ),
                GameEntity(
                    id = 3,
                    title = "Star Fighter 64 (Space Combat)",
                    consoleType = ConsoleType.N64,
                    filePath = "builtin://n64_star_fighter",
                    coverDrawableName = "n64_game_hero_1790054771488",
                    fileSizeBytes = 12_500_000L,
                    lastPlayedTimestamp = 0,
                    playTimeSeconds = 0,
                    isFavorite = false,
                    region = "JPN",
                    isBuiltInDemo = true,
                    description = "Combate espacial tático em 3D estilo rail-shooter com mira giroscópica e manobras evasivas."
                ),
                GameEntity(
                    id = 4,
                    title = "Bio Hazard Survival (PSX)",
                    consoleType = ConsoleType.PS1,
                    filePath = "builtin://ps1_bio_hazard",
                    coverDrawableName = "ps1_game_hero_1790054759614",
                    fileSizeBytes = 512_000_000L,
                    lastPlayedTimestamp = 0,
                    playTimeSeconds = 0,
                    isFavorite = false,
                    region = "EUR",
                    isBuiltInDemo = true,
                    description = "Aventura de sobrevivência clássica de PS1 com câmeras fixas dinâmicas, renderização de salas sombrias e controles tipo tanque."
                )
            )
            gameDao.insertAll(defaultGames)
        }
    }
}
