package com.bungamust.deepnoura.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Segment(
    val id: String = UUID.randomUUID().toString(),
    val title: String? = null,
    val body: String,
    @SerialName("durationSec") val durationSec: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class Project(
    val projectName: String = "Untitled",
    val updatedAt: Long = System.currentTimeMillis(),
    val segments: List<Segment> = emptyList()
)
