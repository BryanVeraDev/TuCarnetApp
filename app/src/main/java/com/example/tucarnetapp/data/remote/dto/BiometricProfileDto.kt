package com.example.tucarnetapp.data.remote.dto

import com.example.tucarnetapp.data.remote.dto.enums.BiometricStatus

data class BiometricProfileDto(
    val student_id: String,
    val status: BiometricStatus,
    val created_at: String,
    val validations: List<BiometricValidationLogDto>
)
