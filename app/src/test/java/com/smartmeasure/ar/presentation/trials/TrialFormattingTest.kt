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

    "path meters keeps one decimal, rounding half away from zero" {
        TrialFormatting.pathMeters(4.25, ptBr) shouldBe "4,3"
        TrialFormatting.pathMeters(4.24, en) shouldBe "4.2"
        TrialFormatting.pathMeters(0.0, ptBr) shouldBe "0,0"
    }

    "whole percent converts a 0..1 share to an integer percentage" {
        TrialFormatting.wholePercent(0.92, ptBr) shouldBe "92"
        TrialFormatting.wholePercent(0.0, en) shouldBe "0"
        TrialFormatting.wholePercent(1.0, en) shouldBe "100"
    }

    "whole percent rounds down so 100 only appears when tracking never dropped" {
        TrialFormatting.wholePercent(0.996, ptBr) shouldBe "99"
        TrialFormatting.wholePercent(0.929, en) shouldBe "92"
    }
})
