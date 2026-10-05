package com.smartmeasure.ar.domain.model

import kotlin.math.hypot

data class Point2D(
    val xMeters: Double,
    val yMeters: Double,
)

data class RoomGeometry(
    val vertices: List<Point2D>,
) {
    init {
        require(vertices.size >= 3) { "A room requires at least three vertices." }
    }

    val perimeterMeters: Double
        get() = vertices.indices.sumOf { index ->
            val current = vertices[index]
            val next = vertices[(index + 1) % vertices.size]
            hypot(next.xMeters - current.xMeters, next.yMeters - current.yMeters)
        }

    val areaSquareMeters: Double
        get() {
            val doubledSignedArea = vertices.indices.sumOf { index ->
                val current = vertices[index]
                val next = vertices[(index + 1) % vertices.size]
                current.xMeters * next.yMeters - next.xMeters * current.yMeters
            }
            return kotlin.math.abs(doubledSignedArea) / 2.0
        }

    companion object {
        fun rectangle(widthMeters: Double, lengthMeters: Double): RoomGeometry {
            require(widthMeters > 0) { "Width must be greater than zero." }
            require(lengthMeters > 0) { "Length must be greater than zero." }

            return RoomGeometry(
                vertices = listOf(
                    Point2D(0.0, 0.0),
                    Point2D(widthMeters, 0.0),
                    Point2D(widthMeters, lengthMeters),
                    Point2D(0.0, lengthMeters),
                ),
            )
        }
    }
}
