package com.example.tucarnetapp.data.remote

import com.example.tucarnetapp.data.remote.api.LivenessApi
import com.example.tucarnetapp.data.remote.api.AuthApi
import com.example.tucarnetapp.data.remote.api.PhotoRequestApi
import com.example.tucarnetapp.data.remote.api.QrApi
import com.example.tucarnetapp.data.remote.api.StudentApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // Emulador: reemplaza por IP real si usas celular físico
    //private const val BASE_URL_LIVENESS = "http://192.168.1.47:4000"
    private const val BASE_URL_LIVENESS = "https://livenesstucarnetservice-production.up.railway.app"
    private const val BASE_URL_BACKEND = "https://tucarnetbe-production.up.railway.app/api/"
    private val okHttp = OkHttpClient.Builder().build()

    val livenessApi: LivenessApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_LIVENESS)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LivenessApi::class.java)
    }

    val authApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_BACKEND)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    val qrApi: QrApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_BACKEND)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QrApi::class.java)
    }

    val studentApi: StudentApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_BACKEND)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StudentApi::class.java)
    }

    val photoRequestApi: PhotoRequestApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_BACKEND)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PhotoRequestApi::class.java)
    }
}
