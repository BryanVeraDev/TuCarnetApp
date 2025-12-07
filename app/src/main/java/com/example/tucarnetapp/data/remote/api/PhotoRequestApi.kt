package com.example.tucarnetapp.data.remote.api

import com.example.tucarnetapp.data.remote.dto.CanRequestPhotoResponse
import com.example.tucarnetapp.data.remote.dto.CreatePhotoRequest
import com.example.tucarnetapp.data.remote.dto.CreatePhotoResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PhotoRequestApi {
    @POST("photo-request")
    suspend fun createPhotoRequest (
        @Body request: CreatePhotoRequest
    ): CreatePhotoResponse

    @GET("photo-request/can-request/{student_id}")
    suspend fun canRequestPhotoUpdate(
        @Path("student_id") studentId: String
    ): CanRequestPhotoResponse
}