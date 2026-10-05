package com.smartmeasure.ar.domain.repository

import com.smartmeasure.ar.domain.model.ArAvailability

fun interface ArAvailabilityRepository {
    fun currentAvailability(): ArAvailability
}

