package com.example.tucarnetapp.data.remote.api

import com.example.tucarnetapp.data.remote.dto.AuthRequest
import com.example.tucarnetapp.data.remote.dto.StudentResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(
        @Header("Authorization") authHeader: String,
        @Body request: AuthRequest
    ): Response<StudentResponse>
}