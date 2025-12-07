package com.example.tucarnetapp.data.remote.dto

data class CanRequestPhotoResponse(
    val canRequest: Boolean,
    val daysRemaining: Int,
    val nextAvailableDate: String? // viene como ISO string
)
