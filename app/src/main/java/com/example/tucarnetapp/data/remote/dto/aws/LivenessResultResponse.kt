package com.example.tucarnetapp.data.remote.dto.aws

data class LivenessResultResponse(
    val status: String,
    val photoKey: String? = null,
    val confidenceScore: Double? = null,
    val referenceImageBase64: String? = null
)
