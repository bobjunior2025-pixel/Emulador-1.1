package com.example.emulator.core

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.example.audio.RetroAudioSynthesizer
import com.example.controller.GamepadState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

class PS1Engine(private val audio: RetroAudioSynthesizer) {

    // Car and Simulation State
    var playerX: Float = 0f
    var speed: Float = 0f
    var maxSpeed: Float = 240f
    var trackZ: Float = 0f
    var steerAngle: Float = 0f
    var gear: Int = 1
    var lap: Int = 1
    var lapProgress: Float = 0f
    var cameraMode: Int = 0 // 0: Chase, 1: Cockpit
    var cdRomSpinning: Boolean = true

    // Frame & Rendering
    var jitterEnabled: Boolean = true
    private var engineSoundCooldown = 0

    // Paints
    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val carPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        typeface = android.graphics.Typeface.MONOSPACE
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }
    private val path = Path()

    fun reset() {
        playerX = 0f
        speed = 0f
        trackZ = 0f
        gear = 1
        lap = 1
        lapProgress = 0f
    }

    fun update(gamepad: GamepadState, dt: Float) {
        // PS1 Cross button or R2: Accelerate
        val isAccelerating = gamepad.ps1Cross || gamepad.r2 || gamepad.rightTrigger > 0.3f
        val isBraking = gamepad.ps1Square || gamepad.l2 || gamepad.leftTrigger > 0.3f
        val isBoosting = gamepad.ps1Circle

        // Steering via Left Stick or D-Pad
        var steerInput = gamepad.leftStickX
        if (gamepad.dpadLeft) steerInput = -1f
        if (gamepad.dpadRight) steerInput = 1f

        val accelRate = if (isBoosting) 140f else 85f
        val topSpeed = if (isBoosting) 310f else maxSpeed

        if (isAccelerating) {
            speed += accelRate * dt
            if (speed > topSpeed) speed = topSpeed
        } else if (isBraking) {
            speed -= 160f * dt
            if (speed < 0f) speed = 0f
        } else {
            // Natural friction
            speed -= 35f * dt
            if (speed < 0f) speed = 0f
        }

        // Automatic/Manual gear simulation
        gear = when {
            speed < 50f -> 1
            speed < 110f -> 2
            speed < 170f -> 3
            speed < 230f -> 4
            else -> 5
        }

        // Steer car
        steerAngle = steerInput * 0.8f
        playerX += steerInput * (speed / maxSpeed) * 320f * dt
        playerX = playerX.coerceIn(-1.8f, 1.8f)

        // Advance track
        trackZ += speed * dt * 2.5f
        lapProgress += speed * dt * 0.005f
        if (lapProgress >= 100f) {
            lapProgress = 0f
            lap++
            audio.triggerVibration(60, 200)
        }

        // Camera toggle
        if (gamepad.ps1Triangle) {
            cameraMode = (cameraMode + 1) % 2
        }

        // Audio pulses
        engineSoundCooldown++
        if (engineSoundCooldown >= 8 && speed > 5f) {
            engineSoundCooldown = 0
            audio.playEnginePulse(speed / topSpeed)
        }
    }

    fun render(canvas: Canvas, width: Float, height: Float, scanlines: Boolean, dithering: Boolean) {
        // Retro Sky gradient (PlayStation Synth Dusk)
        canvas.drawColor(Color.rgb(18, 12, 38))

        // Sun / Neon horizon
        val horizonY = height * 0.42f
        val skyPaint = Paint().apply {
            color = Color.rgb(255, 45, 120)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(width * 0.5f, horizonY - 30f, 65f, skyPaint)

        // Mountains / Distant low poly grid
        val mountainPaint = Paint().apply {
            color = Color.rgb(40, 25, 70)
            style = Paint.Style.FILL
        }
        val mountainPath = Path().apply {
            moveTo(0f, horizonY)
            var mx = 0f
            while (mx <= width) {
                lineTo(mx + 40f, horizonY - 35f - (sin(mx * 0.02f) * 20f))
                lineTo(mx + 80f, horizonY)
                mx += 80f
            }
            lineTo(width, horizonY)
            close()
        }
        canvas.drawPath(mountainPath, mountainPaint)

        // Render 3D Road using pseudo-3D polygon scanlines
        val totalSegments = 160
        val segmentLength = 200f
        val startPos = (trackZ / segmentLength).toInt()

        for (n in totalSegments downTo 1) {
            val segIndex = startPos + n
            val z1 = (n * segmentLength) - (trackZ % segmentLength)
            val z2 = z1 + segmentLength

            if (z1 <= 10f) continue

            // Perspective projection with optional PS1 integer jitter
            var p1 = project(z1, width, height, horizonY, playerX)
            var p2 = project(z2, width, height, horizonY, playerX)

            if (jitterEnabled) {
                p1 = applyPs1Jitter(p1)
                p2 = applyPs1Jitter(p2)
            }

            // Alternating checkerboard road strips (iconic retro racer)
            val isEven = (segIndex / 4) % 2 == 0
            val grassColor = if (isEven) Color.rgb(15, 60, 30) else Color.rgb(20, 75, 40)
            val roadColor = if (isEven) Color.rgb(65, 68, 75) else Color.rgb(52, 54, 60)
            val rumbleColor = if (isEven) Color.rgb(220, 30, 40) else Color.rgb(240, 240, 240)

            // Draw Grass / Terrain
            roadPaint.color = grassColor
            canvas.drawRect(0f, p2.screenY, width, p1.screenY, roadPaint)

            // Draw Rumble strips
            drawTrapezoid(canvas, rumbleColor, p1.screenX, p1.screenY, p1.roadW * 1.18f, p2.screenX, p2.screenY, p2.roadW * 1.18f)

            // Draw Road
            drawTrapezoid(canvas, roadColor, p1.screenX, p1.screenY, p1.roadW, p2.screenX, p2.screenY, p2.roadW)

            // Center lane markings
            if (isEven) {
                drawTrapezoid(canvas, Color.rgb(245, 230, 80), p1.screenX, p1.screenY, p1.roadW * 0.04f, p2.screenX, p2.screenY, p2.roadW * 0.04f)
            }
        }

        // Draw Player's Low-Poly 3D Sports Car
        if (cameraMode == 0) {
            renderLowPolyCar(canvas, width * 0.5f, height * 0.78f, width, height)
        }

        // CRT Scanline Shader Simulation
        if (scanlines) {
            val scanPaint = Paint().apply {
                color = Color.argb(45, 0, 0, 0)
                strokeWidth = 2.5f
            }
            var y = 0f
            while (y < height) {
                canvas.drawLine(0f, y, width, y, scanPaint)
                y += 5f
            }
        }

        // HUD: Speedometer, Lap, Time, PS1 CD Spindle
        renderHud(canvas, width, height)
    }

    private data class ProjectedPoint(val screenX: Float, val screenY: Float, val roadW: Float)

    private fun project(z: Float, width: Float, height: Float, horizonY: Float, pX: Float): ProjectedPoint {
        val fov = 350f
        val scale = fov / z
        val screenX = (width * 0.5f) - (pX * scale * 260f)
        val screenY = horizonY + (scale * 240f)
        val roadW = scale * 1200f
        return ProjectedPoint(screenX, screenY, roadW)
    }

    private fun applyPs1Jitter(pt: ProjectedPoint): ProjectedPoint {
        // PS1 fixed-point truncated coordinate jitter
        val jitterX = (pt.screenX * 0.5f).roundToInt() * 2f
        val jitterY = (pt.screenY * 0.5f).roundToInt() * 2f
        val jitterW = (pt.roadW * 0.5f).roundToInt() * 2f
        return ProjectedPoint(jitterX, jitterY, jitterW)
    }

    private fun drawTrapezoid(canvas: Canvas, color: Int, x1: Float, y1: Float, w1: Float, x2: Float, y2: Float, w2: Float) {
        roadPaint.color = color
        path.reset()
        path.moveTo(x1 - w1, y1)
        path.lineTo(x1 + w1, y1)
        path.lineTo(x2 + w2, y2)
        path.lineTo(x2 - w2, y2)
        path.close()
        canvas.drawPath(path, roadPaint)
    }

    private fun renderLowPolyCar(canvas: Canvas, cx: Float, cy: Float, width: Float, height: Float) {
        val carScale = width * 0.0016f
        val tilt = steerAngle * 14f

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(tilt)

        // Shadow
        val shadowPaint = Paint().apply {
            color = Color.argb(120, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawOval(-60f * carScale, 30f * carScale, 60f * carScale, 55f * carScale, shadowPaint)

        // Car Main Body (Futuristic 90s Low Poly Sports Coupe)
        carPaint.color = Color.rgb(220, 30, 45) // Classic PlayStation Red
        path.reset()
        path.moveTo(-45f * carScale, 20f * carScale)
        path.lineTo(45f * carScale, 20f * carScale)
        path.lineTo(35f * carScale, -15f * carScale)
        path.lineTo(-35f * carScale, -15f * carScale)
        path.close()
        canvas.drawPath(path, carPaint)

        // Rear Wing
        carPaint.color = Color.rgb(30, 32, 40)
        canvas.drawRect(-48f * carScale, -28f * carScale, 48f * carScale, -20f * carScale, carPaint)

        // Cockpit / Glass
        carPaint.color = Color.rgb(30, 45, 75)
        path.reset()
        path.moveTo(-28f * carScale, -12f * carScale)
        path.lineTo(28f * carScale, -12f * carScale)
        path.lineTo(20f * carScale, -35f * carScale)
        path.lineTo(-20f * carScale, -35f * carScale)
        path.close()
        canvas.drawPath(path, carPaint)

        // Tail Lights (Neon Glow)
        val tailLightPaint = Paint().apply {
            color = Color.rgb(255, 60, 60)
            style = Paint.Style.FILL
            setShadowLayer(8f, 0f, 0f, Color.RED)
        }
        canvas.drawRect(-40f * carScale, 8f * carScale, -22f * carScale, 16f * carScale, tailLightPaint)
        canvas.drawRect(22f * carScale, 8f * carScale, 40f * carScale, 16f * carScale, tailLightPaint)

        canvas.restore()
    }

    private fun renderHud(canvas: Canvas, width: Float, height: Float) {
        // Speed Display
        textPaint.color = Color.rgb(255, 235, 60)
        textPaint.textSize = 42f
        canvas.drawText("${speed.toInt()} KM/H", 32f, 70f, textPaint)

        // Lap & Time
        textPaint.color = Color.WHITE
        textPaint.textSize = 28f
        canvas.drawText("VOLTA: $lap/3", 32f, 115f, textPaint)
        canvas.drawText("MARCHA: $gear", 32f, 155f, textPaint)

        // CD-ROM activity indicator (PS1 authentic)
        val ledPaint = Paint().apply {
            color = if (cdRomSpinning) Color.rgb(50, 255, 100) else Color.rgb(80, 80, 80)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(width - 45f, 45f, 9f, ledPaint)
        textPaint.textSize = 22f
        textPaint.color = Color.rgb(180, 190, 210)
        canvas.drawText("DISC 1", width - 135f, 52f, textPaint)
    }
}
