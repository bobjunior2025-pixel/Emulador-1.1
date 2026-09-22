package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RetroPlay 64 & PSX", appName)
  }

  @Test
  fun `verify console types and gamepad state`() {
    val gamepadState = com.example.controller.GamepadState()
    assertEquals(false, gamepadState.isControllerConnected)
    assertEquals(com.example.data.model.ConsoleType.PS1.badge, "PS1")
    assertEquals(com.example.data.model.ConsoleType.N64.badge, "N64")
  }

  @Test
  fun `verify room save state insertion and retrieval`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repo = com.example.data.repository.GameRepository(db.gameDao(), db.saveStateDao())

    val testState = com.example.data.model.SaveStateEntity(
        gameId = 1L,
        slot = 1,
        timestamp = 1000L,
        summary = "Volta 1 | 200 km/h",
        score = 50,
        levelOrStage = "Volta 1/3",
        simulationDataJson = "10.0,150.0,30.0,3,1,0.5"
    )
    val id = repo.saveState(testState)
    assert(id > 0)

    val loaded = repo.loadState(1L, 1)
    org.junit.Assert.assertNotNull(loaded)
    assertEquals("Volta 1 | 200 km/h", loaded?.summary)
    assertEquals(1, loaded?.slot)

    // Overwrite slot 1
    val updatedState = testState.copy(summary = "Volta 2 | 220 km/h")
    repo.saveState(updatedState)
    val reloaded = repo.loadState(1L, 1)
    assertEquals("Volta 2 | 220 km/h", reloaded?.summary)

    // Delete slot 1
    repo.deleteStateBySlot(1L, 1)
    val afterDelete = repo.loadState(1L, 1)
    org.junit.Assert.assertNull(afterDelete)

    db.close()
  }
}
