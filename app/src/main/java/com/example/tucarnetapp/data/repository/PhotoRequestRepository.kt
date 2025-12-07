package com.example.tucarnetapp.data.repository

import com.example.tucarnetapp.data.remote.ApiClient
import com.example.tucarnetapp.data.remote.dto.CanRequestPhotoResponse
import com.example.tucarnetapp.data.remote.dto.CreatePhotoRequest
import com.example.tucarnetapp.data.remote.dto.CreatePhotoResponse

class PhotoRequestRepository {

    private val api = ApiClient.photoRequestApi
    /**
     * Verifica si el estudiante puede solicitar cambio de foto
     */
    suspend fun canRequestPhotoUpdate(
        studentId: String
    ): CanRequestPhotoResponse {
        return api.canRequestPhotoUpdate(studentId)
    }

    /**
     * Crea una solicitud de actualizar foto
     */
    suspend fun createPhotoRequest(
        studentId: String,
        new_photo_url: String
    ): CreatePhotoResponse {
        val request = CreatePhotoRequest(
            student_id = studentId,
            new_photo_url = new_photo_url
        )

        return api.createPhotoRequest(request)
    }
}
