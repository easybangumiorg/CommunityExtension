package io.github.peacefulprogram.easybangumi_nivod.dto

import com.google.gson.annotations.SerializedName

data class VideoStreamUrlResponse(
    @SerializedName("entity")
    val entity: VideoStreamUrlEntity,
    @SerializedName("msg")
    val msg: String,
    @SerializedName("status")
    val status: Int
)

data class VideoStreamUrlEntity(
    @SerializedName("playType")
    val playType: Int,
    @SerializedName("playUrl")
    val playUrl: String
)