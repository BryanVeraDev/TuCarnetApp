package com.example.tucarnetapp.data

data class StudentData(
    val name: String,
    val code: String,
    val documentId: String, // CC
    val career: String,
    val status: String,
    val studentType: String = "ESTUDIANTE",
    val profileImageUrl: String? = null,
    val qrCodeUrl: String? = null
)
