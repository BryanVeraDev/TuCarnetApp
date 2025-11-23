package com.example.tucarnetapp.data.remote.dto

data class QRResponse(
    val qr: String,           // Base64 del QR
    val expiresIn: Int,
    val jwt: String,
    val isNew: Boolean
)
data class StudentValidationData(
    val student_id: String,
    val student_code: String,
    val name: String,
    val last_name: String,
    val email: String,
    val career: String,
    val status: String,           // MATRICULADO o NO_ACTIVO
    val student_type: String,     // PREGRADO o POSGRADO
    val card_photo_url: String?
)
data class ValidateQRResponse(
    val valid: Boolean,
    val student: StudentValidationData?
)