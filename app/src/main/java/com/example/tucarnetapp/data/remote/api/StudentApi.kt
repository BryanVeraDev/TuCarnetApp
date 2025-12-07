package com.example.tucarnetapp.data.remote.api

import com.example.tucarnetapp.data.remote.dto.UpdateBiometricRequest
import com.example.tucarnetapp.data.remote.dto.UpdateBiometricResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PATCH

interface StudentApi {

    @PATCH("student/biometric/validate")
    suspend fun updateBiometricProfile(
        @Body request: UpdateBiometricRequest
    ): UpdateBiometricResponse
}
