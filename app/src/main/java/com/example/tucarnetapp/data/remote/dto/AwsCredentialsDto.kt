package com.example.tucarnetapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AwsCredentialsDto(
    @SerializedName("AccessKeyId")
    val accessKeyId: String,
    @SerializedName("SecretAccessKey")
    val secretAccessKey: String,
    @SerializedName("SessionToken")
    val sessionToken: String,
    @SerializedName("Expiration")
    val expiration: String

)
