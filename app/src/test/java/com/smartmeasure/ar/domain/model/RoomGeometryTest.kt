package com.smartmeasure.ar.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeExactly

class RoomGeometryTest : StringSpec({
    "rectangle calculates area and perimeter" {
        val room = RoomGeometry.rectangle(widthMeters = 4.0, lengthMeters = 3.0)

        room.areaSquareMeters.shouldBeExactly(12.0)
        room.perimeterMeters.shouldBeExactly(14.0)
    }

    "polygon area works regardless of vertex direction" {
        val room = RoomGeometry(
            listOf(
                Point2D(0.0, 0.0),
                Point2D(0.0, 3.0),
                Point2D(4.0, 3.0),
                Point2D(4.0, 0.0),
            ),
        )

        room.areaSquareMeters.shouldBeExactly(12.0)
    }

    "rectangle rejects non-positive dimensions" {
        shouldThrow<IllegalArgumentException> {
            RoomGeometry.rectangle(widthMeters = 0.0, lengthMeters = 3.0)
        }
    }
})
