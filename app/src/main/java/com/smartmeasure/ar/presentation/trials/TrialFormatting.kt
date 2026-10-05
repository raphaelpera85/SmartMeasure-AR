package com.smartmeasure.ar.presentation.trials

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Pure number formatting for the accuracy-trials screen. Units and symbols ("cm", "%", "m") live in
 * strings.xml so translators control their placement; these functions return only the number.
 *
 * Values are rounded half away from zero on the shortest decimal form of the double, so 0.0005 m
 * shows as 0.1 cm. A value that rounds to zero is shown as plain zero, never "-0.0" or "+0.0":
 * a sign on zero suggests a bias the data does not show.
 */
object TrialFormatting {
    private const val CM_DECIMALS = 1
    private const val PERCENT_DECIMALS = 1
    private const val M_DECIMALS = 3

    fun meters(valueMeters: Double, locale: Locale): String =
        format(round(BigDecimal.valueOf(valueMeters), M_DECIMALS), locale, signed = false)

    fun centimeters(valueMeters: Double, locale: Locale): String =
        format(round(BigDecimal.valueOf(valueMeters).movePointRight(2), CM_DECIMALS), locale, signed = false)

    fun signedCentimeters(valueMeters: Double, locale: Locale): String =
        format(round(BigDecimal.valueOf(valueMeters).movePointRight(2), CM_DECIMALS), locale, signed = true)

    fun percent(fraction: Double, locale: Locale): String =
        format(round(BigDecimal.valueOf(fraction).movePointRight(2), PERCENT_DECIMALS), locale, signed = false)

    private fun round(value: BigDecimal, decimals: Int): BigDecimal {
        val rounded = value.setScale(decimals, RoundingMode.HALF_UP)
        return if (rounded.signum() == 0) BigDecimal.ZERO.setScale(decimals) else rounded
    }

    private fun format(value: BigDecimal, locale: Locale, signed: Boolean): String {
        val pattern = if (signed && value.signum() != 0) "%+.${value.scale()}f" else "%.${value.scale()}f"
        return String.format(locale, pattern, value)
    }
}
