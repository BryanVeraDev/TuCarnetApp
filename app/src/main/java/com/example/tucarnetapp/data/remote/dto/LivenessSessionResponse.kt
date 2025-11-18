package com.example.tucarnetapp.data.remote.dto

data class LivenessSessionResponse(
    val sessionId: String,
    val credentials: AwsCredentialsDto
)
