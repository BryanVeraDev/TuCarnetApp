package com.example.tucarnetapp.data.remote.dto.aws

data class CompareFacesRequest(
    val sourceImageBase64: String,
    val targetImageBase64: String
)