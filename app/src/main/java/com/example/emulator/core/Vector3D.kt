package com.example.emulator.core

import kotlin.math.cos
import kotlin.math.sin

data class Vector3D(var x: Float, var y: Float, var z: Float) {
    fun plus(other: Vector3D): Vector3D = Vector3D(x + other.x, y + other.y, z + other.z)
    fun minus(other: Vector3D): Vector3D = Vector3D(x - other.x, y - other.y, z - other.z)
    fun times(scalar: Float): Vector3D = Vector3D(x * scalar, y * scalar, z * scalar)

    fun rotateY(angleRad: Float): Vector3D {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return Vector3D(
            x * cosA - z * sinA,
            y,
            x * sinA + z * cosA
        )
    }

    fun rotateX(angleRad: Float): Vector3D {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        return Vector3D(
            x,
            y * cosA - z * sinA,
            y * sinA + z * cosA
        )
    }
}

data class Polygon3D(
    val v1: Vector3D,
    val v2: Vector3D,
    val v3: Vector3D,
    val color: Int, // ARGB
    var avgZ: Float = 0f
)
