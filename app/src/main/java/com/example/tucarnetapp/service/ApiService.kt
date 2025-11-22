package com.example.tucarnetapp.service

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val idToken: String)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val userStatus: String? = null, // "pending", "approved", "rejected"
    val userData: UserData? = null
)

data class UserData(
    val email: String,
    val name: String,
    val photoUrl: String?
)

interface ApiService {
    @POST("auth/login") // Ajusta la ruta según tu backend
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}