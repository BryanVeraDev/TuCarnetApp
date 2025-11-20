package com.example.tucarnetapp.data.repository

import com.example.tucarnetapp.data.remote.ApiClient
import com.example.tucarnetapp.data.remote.dto.LivenessSessionResponse

class LivenessRepository {

    suspend fun createLivenessSession(): LivenessSessionResponse {
        return ApiClient.livenessApi.createSession()
    }
}
