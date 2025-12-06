package com.example.tucarnetapp.data.remote.dto.aws

data class FaceMatch(
    val similarity: Double,
    val boundingBox: BoundingBox
)