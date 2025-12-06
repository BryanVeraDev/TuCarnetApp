package com.example.tucarnetapp.data.remote.dto.aws

import com.google.gson.annotations.SerializedName

data class TemporaryCredentials(
    @SerializedName("AccessKeyId")
    val accessKeyId: String,
    @SerializedName("SecretAccessKey")
    val secretAccessKey: String,
    @SerializedName("SessionToken")
    val sessionToken: String,
    @SerializedName("Expiration")
    val expiration: String

)
