package com.smartmeasure.ar.presentation.manual

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class ManualMeasurementViewModelTest : StringSpec({
    "starts without error or geometry" {
        val state = ManualMeasurementViewModel().uiState.value

        state.inputError shouldBe null
        state.geometry shouldBe null
    }

    "empty fields produce a typed INVALID_DIMENSIONS error and no geometry" {
        val vm = ManualMeasurementViewModel()

        vm.calculateRectangle()

        vm.uiState.value.inputError shouldBe ManualInputError.INVALID_DIMENSIONS
        vm.uiState.value.geometry shouldBe null
    }

    "non-numeric or non-positive width produces INVALID_DIMENSIONS and no geometry" {
        listOf("abc", "0", "-2", "0,0").forEach { width ->
            val vm = ManualMeasurementViewModel()
            vm.onWidthChanged(width)
            vm.onLengthChanged("3")

            vm.calculateRectangle()

            vm.uiState.value.inputError shouldBe ManualInputError.INVALID_DIMENSIONS
            vm.uiState.value.geometry shouldBe null
        }
    }

    "valid dimensions after an error clear it and produce the geometry" {
        val vm = ManualMeasurementViewModel()
        vm.calculateRectangle()
        vm.onWidthChanged("2,5")
        vm.onLengthChanged("4")

        vm.calculateRectangle()

        vm.uiState.value.inputError shouldBe null
        val geometry = vm.uiState.value.geometry.shouldNotBeNull()
        geometry.areaSquareMeters shouldBe (10.0 plusOrMinus 1e-9)
        geometry.perimeterMeters shouldBe (13.0 plusOrMinus 1e-9)
    }

    "editing a field clears a previous error" {
        val vm = ManualMeasurementViewModel()
        vm.calculateRectangle()

        vm.onWidthChanged("3")

        vm.uiState.value.inputError shouldBe null
    }
})
