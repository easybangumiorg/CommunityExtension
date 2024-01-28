package io.github.peacefulprogram.easybangumi_nivod.dto

import com.google.gson.annotations.SerializedName


data class VideoPlayLang(
    @SerializedName("langId")
    val langId: Int,
    @SerializedName("langName")
    val langName: String
)


data class VideoPlaySource(
    @SerializedName("sourceId")
    val sourceId: Int,
    @SerializedName("sourceName")
    val sourceName: String
)