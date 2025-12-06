package com.example.tucarnetapp.data

data class StudentData(
    val name: String,
    val code: String,
    val career: String,
    val status: String,
    val studentType: String = "ESTUDIANTE",
    val profileImageKey: String? = null,
    val qrCodeUrl: String? = null
)
