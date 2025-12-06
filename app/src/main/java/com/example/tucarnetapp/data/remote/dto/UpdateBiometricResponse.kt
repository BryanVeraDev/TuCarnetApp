package com.example.tucarnetapp.data.remote.dto

data class UpdateBiometricResponse(
    val success: Boolean,
    val message: String,
    val student: StudentDto?,
    val validation: ValidationDto
)
