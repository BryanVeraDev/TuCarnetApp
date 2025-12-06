package com.example.tucarnetapp.data.repository

import com.example.tucarnetapp.data.remote.ApiClient
import com.example.tucarnetapp.data.remote.dto.aws.CompareFacesRequest
import com.example.tucarnetapp.data.remote.dto.aws.CompareFacesResponse
import com.example.tucarnetapp.data.remote.dto.aws.GetPhotoRequest
import com.example.tucarnetapp.data.remote.dto.aws.LivenessResultResponse
import com.example.tucarnetapp.data.remote.dto.aws.LivenessSessionResponse
import com.example.tucarnetapp.data.remote.dto.aws.PhotoUrlResponse
import com.example.tucarnetapp.data.remote.dto.aws.UploadPhotoRequest
import com.example.tucarnetapp.data.remote.dto.aws.UploadPhotoResponse

class LivenessRepository {

    private val api = ApiClient.livenessApi

    /**
     * POST /liveness/start
     */
    suspend fun startLiveness(): LivenessSessionResponse {
        return api.startLiveness()
    }

    /**
     * GET /liveness/result/{sessionId}
     */
    suspend fun getLivenessResult(sessionId: String): LivenessResultResponse {
        return api.getResult(sessionId)
    }

    /**
     * GET /liveness/photo
     */
    suspend fun getPhotoUrl(photoKey: String): PhotoUrlResponse {
        val request = GetPhotoRequest(
            photoKey = photoKey
        )
        return api.getPhoto(request)
    }

    /**
     * POST /liveness/photo
     */
    suspend fun uploadPhotoBase64(imageBase64: String): UploadPhotoResponse {
        val request = UploadPhotoRequest(
            imageBase64 = imageBase64
        )
        return api.uploadPhoto(request)

    }

    /**
     * POST /liveness/compare
     */
    suspend fun compareFaces(
        sourceImageBase64: String,
        targetImageBase64: String
    ): CompareFacesResponse {
        val request = CompareFacesRequest(
            sourceImageBase64 = sourceImageBase64,
            targetImageBase64 = targetImageBase64
        )
        return api.compareFaces(request)
    }
}
