package com.example.tucarnetapp.data.remote.dto

data class CreateQrDto(
    val student_code: String
)

data class ValidateQrDto(
    val token: String
)
