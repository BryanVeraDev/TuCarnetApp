package com.example.tucarnetapp.data.remote.dto

import com.example.tucarnetapp.data.remote.dto.enums.StudentStatus
import com.example.tucarnetapp.data.remote.dto.enums.StudentType

data class StudentDto(
    val student_id: String,
    val firebase_id: String,
    val student_code: String,
    val card_photo_key: String?,
    val email: String,
    val name: String,
    val last_name: String,
    val student_type: StudentType,
    val career: String,
    val status: StudentStatus,
    val created_at: String,
    val lastSyncAt: String,
    val updated_at: String?,
    val biometric_profile: BiometricProfileDto?
)
