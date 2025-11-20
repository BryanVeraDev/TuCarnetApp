package com.example.tucarnetapp.data.remote

import com.example.tucarnetapp.data.remote.api.LivenessApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // Emulador: reemplaza por IP real si usas celular físico
    //private const val BASE_URL_LIVENESS = "http://192.168.1.47:4000"
    private const val BASE_URL_LIVENESS = "https://livenesstucarnetservice-production.up.railway.app"

    val livenessApi: LivenessApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_LIVENESS)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LivenessApi::class.java)
    }
}
