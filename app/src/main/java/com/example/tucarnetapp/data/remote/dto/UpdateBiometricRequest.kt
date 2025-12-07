package com.example.tucarnetapp.data.remote.dto

data class UpdateBiometricRequest(
    val student_id: String,
    val card_photo_key: String,
    val similarity: Double
)
