package com.smartmeasure.ar.presentation.trials

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

class TrialFormattingTest : StringSpec({
    val ptBr = Locale.forLanguageTag("pt-BR")
    val en = Locale.US

    "centimeters converts meters to one decimal using the locale separator" {
        TrialFormatting.centimeters(0.0123, ptBr) shouldBe "1,2"
        TrialFormatting.centimeters(0.0123, en) shouldBe "1.2"
    }

    "centimeters shows a value that rounds to zero without a minus sign" {
        TrialFormatting.centimeters(-0.0004, ptBr) shouldBe "0,0"
        TrialFormatting.centimeters(-0.0, en) shouldBe "0.0"
    }

    "signed centimeters keeps the sign of a non-zero error" {
        TrialFormatting.signedCentimeters(0.0123, ptBr) shouldBe "+1,2"
        TrialFormatting.signedCentimeters(-0.0123, ptBr) shouldBe "-1,2"
    }

    "signed centimeters shows an error that rounds to zero without any sign" {
        TrialFormatting.signedCentimeters(-0.0004, ptBr) shouldBe "0,0"
        TrialFormatting.signedCentimeters(0.0004, ptBr) shouldBe "0,0"
        TrialFormatting.signedCentimeters(0.0, en) shouldBe "0.0"
        TrialFormatting.signedCentimeters(-0.0, en) shouldBe "0.0"
    }

    "signed centimeters rounds half away from zero at the display precision" {
        TrialFormatting.signedCentimeters(0.0005, en) shouldBe "+0.1"
        TrialFormatting.signedCentimeters(-0.0005, en) shouldBe "-0.1"
    }

    "percent converts a fraction to one decimal without the percent sign" {
        TrialFormatting.percent(0.0257, ptBr) shouldBe "2,6"
        TrialFormatting.percent(0.0257, en) shouldBe "2.6"
        TrialFormatting.percent(-0.0001, en) shouldBe "0.0"
    }

    "meters keeps three decimals for millimetre resolution" {
        TrialFormatting.meters(2.5, ptBr) shouldBe "2,500"
        TrialFormatting.meters(2.5, en) shouldBe "2.500"
    }
})
