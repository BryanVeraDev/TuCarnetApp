package com.example.tucarnetapp.data.remote.dto

data class ValidationDto(
    val similarity: Float,
    val result: String,
    val threshold: Int
)
