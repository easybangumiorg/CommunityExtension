package io.github.peacefulprogram.easybangumi_nivod.dto

import com.google.gson.annotations.SerializedName


data class SearchVideoResponse(
    @SerializedName("list")
    val list: List<SearchVideo>,
    @SerializedName("more")
    val more: Int,
    @SerializedName("msg")
    val msg: String,
    @SerializedName("start")
    val start: String,
    @SerializedName("status")
    val status: Int
)

data class SearchVideo(
    @SerializedName("actors")
    val actors: String = "",
    @SerializedName("addDate")
    val addDate: Long,
    @SerializedName("catId")
    val catId: Int,
    @SerializedName("channelId")
    val channelId: Int,
    @SerializedName("channelName")
    val channelName: String,
    @SerializedName("commentCount")
    val commentCount: Int,
    @SerializedName("director")
    val director: String = "",
    @SerializedName("episodesTxt")
    val episodesTxt: String = "",
    @SerializedName("favoriteCount")
    val favoriteCount: Int,
    @SerializedName("hot")
    val hot: Int,
    @SerializedName("inSeries")
    val inSeries: Int,
    @SerializedName("isEpisodes")
    val isEpisodes: Int,
    @SerializedName("isEpisodesEnd")
    val isEpisodesEnd: Int,
    @SerializedName("playLangs")
    val playLangs: List<VideoPlayLang>,
    @SerializedName("playResolutions")
    val playResolutions: List<String>,
    @SerializedName("playSources")
    val playSources: List<VideoPlaySource>,
    @SerializedName("postYear")
    val postYear: Int,
    @SerializedName("rating")
    val rating: Int,
    @SerializedName("regionId")
    val regionId: Int,
    @SerializedName("regionName")
    val regionName: String,
    @SerializedName("shareCount")
    val shareCount: Int,
    @SerializedName("shareForced")
    val shareForced: Int,
    @SerializedName("showId")
    val showId: Int,
    @SerializedName("showIdCode")
    val showIdCode: String,
    @SerializedName("showImg")
    val showImg: String,
    @SerializedName("showTcTitle")
    val showTcTitle: String,
    @SerializedName("showTitle")
    val showTitle: String,
    @SerializedName("showTypeId")
    val showTypeId: Int,
    @SerializedName("showTypeName")
    val showTypeName: String = "",
    @SerializedName("status")
    val status: Int,
    @SerializedName("voteDown")
    val voteDown: Int,
    @SerializedName("voteUp")
    val voteUp: Int
)