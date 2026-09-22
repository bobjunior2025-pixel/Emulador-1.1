package com.example.emulator.core

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.example.audio.RetroAudioSynthesizer
import com.example.controller.GamepadState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class N64Engine(private val audio: RetroAudioSynthesizer) {

    // Player 3D Coordinates & Physics
    var posX: Float = 0f
    var posY: Float = 0f
    var posZ: Float = 0f
    var velX: Float = 0f
    var velY: Float = 0f
    var velZ: Float = 0f
    var playerAngle: Float = 0f
    var isGrounded: Boolean = true

    // Camera 3D
    var cameraAngle: Float = 0f
    var cameraPitch: Float = 0.35f
    var cameraDistance: Float = 280f

    // Game stats
    var starCount: Int = 3
    var coinCount: Int = 18
    var health: Int = 8
    var maxHealth: Int = 8

    // World Entities (Floating Islands, Trees, Coins)
    private val coins = mutableListOf(
        Vector3D(0f, -15f, 120f),
        Vector3D(80f, -15f, 60f),
        Vector3D(-80f, -15f, 60f),
        Vector3D(0f, -15f, -90f),
        Vector3D(120f, -15f, -40f),
        Vector3D(-110f, -15f, -50f)
    )
    private var coinSpinAngle: Float = 0f

    // Rendering Paints
    private val polygonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(100, 0, 0, 0)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 34f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }
    private val path = Path()

    fun reset() {
        posX = 0f
        posY = 0f
        posZ = 0f
        velX = 0f
        velY = 0f
        velZ = 0f
        playerAngle = 0f
        cameraAngle = 0f
        starCount = 3
        coinCount = 18
    }

    fun update(gamepad: GamepadState, dt: Float) {
        // N64 360° Analog Joystick input
        var stickX = gamepad.leftStickX
        var stickY = gamepad.leftStickY

        // Fallback to D-Pad if no analog
        if (stickX == 0f && stickY == 0f) {
            if (gamepad.dpadLeft) stickX = -1f
            if (gamepad.dpadRight) stickX = 1f
            if (gamepad.dpadUp) stickY = -1f
            if (gamepad.dpadDown) stickY = 1f
        }

        val stickMag = sqrt(stickX * stickX + stickY * stickY).coerceAtMost(1f)

        // Camera control: N64 C-Buttons or Right Analog Stick
        val cLeft = gamepad.n64CLeft || gamepad.rightStickX < -0.4f
        val cRight = gamepad.n64CRight || gamepad.rightStickX > 0.4f
        val cUp = gamepad.n64CUp || gamepad.rightStickY < -0.4f
        val cDown = gamepad.n64CDown || gamepad.rightStickY > 0.4f

        if (cLeft) cameraAngle -= 2.2f * dt
        if (cRight) cameraAngle += 2.2f * dt
        if (cUp) cameraDistance = (cameraDistance - 150f * dt).coerceIn(160f, 400f)
        if (cDown) cameraDistance = (cameraDistance + 150f * dt).coerceIn(160f, 400f)

        // Player Movement in camera relative coordinates
        if (stickMag > 0.1f) {
            val inputAngle = atan2(stickX, -stickY)
            val moveAngle = cameraAngle + inputAngle
            playerAngle = moveAngle

            val speed = 210f * stickMag
            velX = sin(moveAngle) * speed
            velZ = cos(moveAngle) * speed
        } else {
            velX *= 0.8f
            velZ *= 0.8f
        }

        // Jump mechanics: N64 'A' Button
        if ((gamepad.n64A || gamepad.ps1Cross) && isGrounded) {
            velY = -340f
            isGrounded = false
            audio.playN64Jump()
            audio.triggerVibration(40, 160)
        }

        // Z-Trigger (Ground Pound / Crouch)
        if (gamepad.n64Z && !isGrounded) {
            velY = 550f // Slam down
        }

        // Apply Gravity
        velY += 800f * dt
        posY += velY * dt
        posX += velX * dt
        posZ += velZ * dt

        // Ground Collision on island
        val islandRadius = 240f
        val distFromCenter = sqrt(posX * posX + posZ * posZ)

        if (distFromCenter < islandRadius) {
            if (posY >= 0f) {
                posY = 0f
                velY = 0f
                isGrounded = true
            }
        } else {
            // Fell off island
            isGrounded = false
            if (posY > 450f) {
                // Respawn
                posX = 0f
                posY = -80f
                posZ = 0f
                velX = 0f
                velY = 0f
                velZ = 0f
                audio.triggerVibration(90, 220)
            }
        }

        // Coin spin & collection
        coinSpinAngle += 2.5f * dt
        val iter = coins.iterator()
        while (iter.hasNext()) {
            val coin = iter.next()
            val dx = posX - coin.x
            val dy = posY - coin.y
            val dz = posZ - coin.z
            val dist = sqrt(dx * dx + dy * dy + dz * dz)
            if (dist < 32f) {
                iter.remove()
                coinCount++
                audio.playN64Coin()
                audio.triggerVibration(35, 180)
            }
        }
    }

    fun render(canvas: Canvas, width: Float, height: Float, scanlines: Boolean, expansionPak: Boolean) {
        // N64 Sky Blue with Atmospheric Fog
        canvas.drawColor(Color.rgb(105, 175, 245))

        val fov = 420f
        val cx = width * 0.5f
        val cy = height * 0.52f

        // Camera position
        val camX = posX - sin(cameraAngle) * cameraDistance
        val camY = posY - (sin(cameraPitch) * cameraDistance) - 40f
        val camZ = posZ - cos(cameraAngle) * cameraDistance

        // Render List for 3D Z-sorting
        val renderList = mutableListOf<Polygon3D>()

        // 1. Floating Main Island (Octagonal Gouraud low-poly terrain)
        val islandRadius = 240f
        val numSides = 8
        for (i in 0 until numSides) {
            val a1 = (i.toFloat() / numSides) * 2f * PI.toFloat()
            val a2 = ((i + 1).toFloat() / numSides) * 2f * PI.toFloat()

            val p0 = Vector3D(0f, 0f, 0f)
            val p1 = Vector3D(cos(a1) * islandRadius, 0f, sin(a1) * islandRadius)
            val p2 = Vector3D(cos(a2) * islandRadius, 0f, sin(a2) * islandRadius)

            val islandColor = if (i % 2 == 0) Color.rgb(65, 175, 75) else Color.rgb(55, 155, 65)
            renderList.add(Polygon3D(p0, p1, p2, islandColor))

            // Island cliff sides
            val p1Bottom = Vector3D(p1.x * 0.8f, 120f, p1.z * 0.8f)
            val p2Bottom = Vector3D(p2.x * 0.8f, 120f, p2.z * 0.8f)
            val rockColor = if (i % 2 == 0) Color.rgb(135, 95, 65) else Color.rgb(115, 80, 55)
            renderList.add(Polygon3D(p1, p2, p1Bottom, rockColor))
            renderList.add(Polygon3D(p2, p2Bottom, p1Bottom, rockColor))
        }

        // 2. Spinning Golden Coins
        for (coin in coins) {
            val cosS = cos(coinSpinAngle) * 14f
            val sinS = sin(coinSpinAngle) * 14f
            val cTop = Vector3D(coin.x, coin.y - 15f, coin.z)
            val cLeft = Vector3D(coin.x - cosS, coin.y, coin.z - sinS)
            val cRight = Vector3D(coin.x + cosS, coin.y, coin.z + sinS)
            val cBottom = Vector3D(coin.x, coin.y + 15f, coin.z)

            val goldColor = Color.rgb(255, 215, 0)
            renderList.add(Polygon3D(cTop, cLeft, cRight, goldColor))
            renderList.add(Polygon3D(cBottom, cRight, cLeft, goldColor))
        }

        // Transform, Depth Sort, and Draw Polygons
        for (poly in renderList) {
            val t1 = transformVertex(poly.v1, camX, camY, camZ)
            val t2 = transformVertex(poly.v2, camX, camY, camZ)
            val t3 = transformVertex(poly.v3, camX, camY, camZ)
            poly.avgZ = (t1.z + t2.z + t3.z) / 3f
        }

        // Painter's Algorithm: Sort Back-to-Front
        renderList.sortByDescending { it.avgZ }

        for (poly in renderList) {
            if (poly.avgZ <= 15f) continue // Clipping plane

            val t1 = transformVertex(poly.v1, camX, camY, camZ)
            val t2 = transformVertex(poly.v2, camX, camY, camZ)
            val t3 = transformVertex(poly.v3, camX, camY, camZ)

            val sx1 = cx + (t1.x / t1.z) * fov
            val sy1 = cy + (t1.y / t1.z) * fov

            val sx2 = cx + (t2.x / t2.z) * fov
            val sy2 = cy + (t2.y / t2.z) * fov

            val sx3 = cx + (t3.x / t3.z) * fov
            val sy3 = cy + (t3.y / t3.z) * fov

            // Distance Fog blend (classic N64 fog)
            val fogFactor = (poly.avgZ / 950f).coerceIn(0f, 1f)
            val finalColor = blendWithFog(poly.color, Color.rgb(105, 175, 245), fogFactor)

            polygonPaint.color = finalColor
            path.reset()
            path.moveTo(sx1, sy1)
            path.lineTo(sx2, sy2)
            path.lineTo(sx3, sy3)
            path.close()
            canvas.drawPath(path, polygonPaint)
        }

        // 3. Render 3D Player Hero
        renderPlayerHero(canvas, camX, camY, camZ, fov, cx, cy)

        // CRT Scanline Shader
        if (scanlines) {
            val scanPaint = Paint().apply {
                color = Color.argb(35, 0, 0, 0)
                strokeWidth = 2.5f
            }
            var y = 0f
            while (y < height) {
                canvas.drawLine(0f, y, width, y, scanPaint)
                y += 5f
            }
        }

        // HUD: Mario 64 Style Stars, Coins, Health Ring, Expansion Pak
        renderHud(canvas, width, height, expansionPak)
    }

    private fun transformVertex(v: Vector3D, camX: Float, camY: Float, camZ: Float): Vector3D {
        val dx = v.x - camX
        val dy = v.y - camY
        val dz = v.z - camZ

        // Yaw rotation around camera
        val cosA = cos(-cameraAngle)
        val sinA = sin(-cameraAngle)
        val rx = dx * cosA - dz * sinA
        val rz = dx * sinA + dz * cosA

        // Pitch rotation
        val cosP = cos(-cameraPitch)
        val sinP = sin(-cameraPitch)
        val ry = dy * cosP - rz * sinP
        val fz = dy * sinP + rz * cosP

        return Vector3D(rx, ry, fz)
    }

    private fun renderPlayerHero(canvas: Canvas, camX: Float, camY: Float, camZ: Float, fov: Float, cx: Float, cy: Float) {
        val pPos = Vector3D(posX, posY, posZ)
        val tPlayer = transformVertex(pPos, camX, camY, camZ)
        if (tPlayer.z <= 10f) return

        val scale = fov / tPlayer.z
        val sx = cx + (tPlayer.x / tPlayer.z) * fov
        val sy = cy + (tPlayer.y / tPlayer.z) * fov

        // Shadow on ground
        val shadowScale = scale * 18f
        canvas.drawOval(sx - shadowScale, sy + (scale * 8f) - (shadowScale * 0.4f), sx + shadowScale, sy + (scale * 8f) + (shadowScale * 0.4f), shadowPaint)

        // 3D Low-Poly Character (N64 Red Cap, Blue Overalls)
        val heroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        // Body (Blue)
        heroPaint.color = Color.rgb(20, 70, 200)
        canvas.drawCircle(sx, sy - (scale * 12f), scale * 14f, heroPaint)

        // Head (Peach skin)
        heroPaint.color = Color.rgb(255, 205, 160)
        canvas.drawCircle(sx, sy - (scale * 26f), scale * 10f, heroPaint)

        // Cap (N64 Red)
        heroPaint.color = Color.rgb(230, 30, 40)
        val capFacingX = sin(playerAngle - cameraAngle) * scale * 5f
        canvas.drawCircle(sx + capFacingX, sy - (scale * 32f), scale * 10f, heroPaint)
    }

    private fun blendWithFog(color: Int, fogColor: Int, factor: Float): Int {
        val r = (Color.red(color) * (1f - factor) + Color.red(fogColor) * factor).toInt()
        val g = (Color.green(color) * (1f - factor) + Color.green(fogColor) * factor).toInt()
        val b = (Color.blue(color) * (1f - factor) + Color.blue(fogColor) * factor).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    private fun renderHud(canvas: Canvas, width: Float, height: Float, expansionPak: Boolean) {
        // N64 Star HUD
        textPaint.color = Color.rgb(255, 220, 40)
        textPaint.textSize = 38f
        canvas.drawText("★ x$starCount", 30f, 65f, textPaint)

        // Coin HUD
        textPaint.color = Color.rgb(255, 210, 0)
        textPaint.textSize = 34f
        canvas.drawText("● x$coinCount", 30f, 110f, textPaint)

        // Health Pie Segment (Iconic Mario 64 Power Meter)
        val hpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(50, 200, 70)
            style = Paint.Style.STROKE
            strokeWidth = 10f
        }
        canvas.drawCircle(width * 0.5f, 50f, 22f, hpPaint)
        textPaint.textSize = 22f
        textPaint.color = Color.WHITE
        canvas.drawText("8", width * 0.5f - 6f, 58f, textPaint)

        // Expansion Pak 8MB Badge
        if (expansionPak) {
            val badgePaint = Paint().apply {
                color = Color.argb(190, 20, 25, 40)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(width - 175f, 25f, width - 25f, 65f, 10f, 10f, badgePaint)
            textPaint.textSize = 20f
            textPaint.color = Color.rgb(255, 60, 60)
            canvas.drawText("EXP. PAK 8MB", width - 165f, 52f, textPaint)
        }
    }
}
