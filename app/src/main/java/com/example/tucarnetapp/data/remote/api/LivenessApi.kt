package com.example.tucarnetapp.data.remote.api

import com.example.tucarnetapp.data.remote.dto.aws.CompareFacesRequest
import com.example.tucarnetapp.data.remote.dto.aws.CompareFacesResponse
import com.example.tucarnetapp.data.remote.dto.aws.LivenessResultResponse
import com.example.tucarnetapp.data.remote.dto.aws.LivenessSessionResponse
import com.example.tucarnetapp.data.remote.dto.aws.PhotoUrlResponse
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


    // GET /liveness/result/{sessionId}
    @GET("/liveness/result/{sessionId}")
    suspend fun getResult(
        @Path("sessionId") sessionId: String
    ): LivenessResultResponse


    // GET /liveness/photo/{photoKey}
    @GET("/liveness/photo/{photoKey}")
    suspend fun getPhoto(
        @Path("photoKey") photoKey: String
    ): PhotoUrlResponse
}
