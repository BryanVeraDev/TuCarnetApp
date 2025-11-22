package com.example.tucarnetapp.data.remote.api

import com.example.tucarnetapp.data.remote.dto.QRResponse
import com.example.tucarnetapp.data.remote.dto.ValidateQRResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface QrApi {
    @POST("qr/generate")
    suspend fun generateQr(
        @Body createQrDto: Map<String, String>
    ): Response<QRResponse>

    @POST("qr/validate")
    suspend fun validateQr(
        @Body validateQrDto: Map<String, String>
    ): Response<ValidateQRResponse>
}