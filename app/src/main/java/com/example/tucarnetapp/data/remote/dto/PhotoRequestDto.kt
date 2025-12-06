package com.example.tucarnetapp.data.remote.dto

import com.example.tucarnetapp.data.remote.dto.enums.PhotoRequestStatus

data class PhotoRequestDto(
    val request_id: String,
    val student_id: String,
    val admin_id: String?,
    val status: PhotoRequestStatus,
    val new_photo_url: String?,
    val application_date: String,
    val response_date: String?
)
