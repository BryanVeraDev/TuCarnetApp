package com.example.tucarnetapp.data.remote.api

import com.example.tucarnetapp.data.remote.dto.aws.CompareFacesRequest
import com.example.tucarnetapp.data.remote.dto.aws.CompareFacesResponse
import com.example.tucarnetapp.data.remote.dto.aws.GetPhotoRequest
import com.example.tucarnetapp.data.remote.dto.aws.LivenessResultResponse
import com.example.tucarnetapp.data.remote.dto.aws.LivenessSessionResponse
import com.example.tucarnetapp.data.remote.dto.aws.PhotoUrlResponse
import com.example.tucarnetapp.data.remote.dto.aws.UploadPhotoRequest
import com.example.tucarnetapp.data.remote.dto.aws.UploadPhotoResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LivenessApi {

    // POST /liveness/start
    @POST("/liveness/start")
    suspend fun startLiveness(): LivenessSessionResponse


    // POST /liveness/compare
    @POST("/liveness/compare")
    suspend fun compareFaces(
        @Body body: CompareFacesRequest
    ): CompareFacesResponse

    // POST /liveness/photo
    @POST("/liveness/photo/upload")
    suspend fun uploadPhoto(
        @Body body: UploadPhotoRequest
    ): UploadPhotoResponse

    // GET /liveness/result/{sessionId}
    @GET("/liveness/result/{sessionId}")
    suspend fun getResult(
        @Path("sessionId") sessionId: String
    ): LivenessResultResponse


    // POST /liveness/photo/signedUrl
    @POST("/liveness/photo/signedUrl")
    suspend fun getPhoto(
        @Body body: GetPhotoRequest
    ): PhotoUrlResponse
}
