package com.example.tucarnetapp.data.remote.dto

data class AuthRequest(
    val uid: String,
    val email: String,
    val name: String
)

data class StudentResponse(
    val student_id: String,
    val firebase_id: String,
    val student_code: String,
    val card_photo_key: String,
    val email: String,
    val name: String,
    val last_name: String,
    val student_type: String,
    val career: String,
    val status: String,
    val created_at: String,
    val lastSyncAt: String,
    val updated_at: String?,
    val biometric_profile: BiometricProfile?
)

data class BiometricProfile(
    val student_id: String,
    val status: String,
    val created_at: String
)
