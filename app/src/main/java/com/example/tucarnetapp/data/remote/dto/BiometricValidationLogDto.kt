package com.example.tucarnetapp.data.remote.dto

import com.example.tucarnetapp.data.remote.dto.enums.ValidationResult

data class BiometricValidationLogDto(
    val validation_id: String,
    val similitarity: Double,
    val result: ValidationResult,
    val created_at: String
)
