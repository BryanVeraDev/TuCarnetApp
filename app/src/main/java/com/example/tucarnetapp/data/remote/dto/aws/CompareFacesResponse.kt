package com.example.tucarnetapp.data.remote.dto.aws

data class CompareFacesResponse(
    val matches: List<FaceMatch>,
    val unmatched: Int
)