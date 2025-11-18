package com.example.tucarnetapp.data.remote.api

import com.example.tucarnetapp.data.remote.dto.LivenessSessionResponse
import retrofit2.http.POST

interface LivenessApi {
    @POST("/liveness/start")
    suspend fun createSession(): LivenessSessionResponse
}
