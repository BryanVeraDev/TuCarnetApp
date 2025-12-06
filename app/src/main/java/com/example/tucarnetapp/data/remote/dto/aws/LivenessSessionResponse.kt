package com.example.tucarnetapp.data.remote.dto.aws

data class LivenessSessionResponse(
    val sessionId: String,
    val credentials: TemporaryCredentials
)
