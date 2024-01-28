package io.github.peacefulprogram.easybangumi_nivod.dto

import com.google.gson.annotations.SerializedName

data class VideoDetailResponse(
    @SerializedName("entity")
    val entity: VideoDetailEntity,
    @SerializedName("msg")
    val msg: String,
    @SerializedName("status")
    val status: Int
)

data class VideoDetailEntity(
    @SerializedName("actors")
    val actors: String,
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
    val director: String,
    @SerializedName("episodesTxt")
    val episodesTxt: String,
    @SerializedName("episodesUpdateDesc")
    val episodesUpdateDesc: String,
    @SerializedName("episodesUpdateRemark")
    val episodesUpdateRemark: String,
    @SerializedName("favoriteCount")
    val favoriteCount: Int,
    @SerializedName("forceApp")
    val forceApp: String,
    @SerializedName("hot")
    val hot: Int,
    @SerializedName("inSeries")
    val inSeries: Int,
    @SerializedName("isEpisodes")
    val isEpisodes: Int,
    @SerializedName("isEpisodesEnd")
    val isEpisodesEnd: Int,
    @SerializedName("pageBgImg")
    val pageBgImg: String,
    @SerializedName("playLangs")
    val playLangs: List<VideoPlayLang>,
    @SerializedName("playResolutions")
    val playResolutions: List<String>,
    @SerializedName("playSources")
    val playSources: List<VideoPlaySource>,
    @SerializedName("plays")
    val plays: List<VideoDetailPlay>,
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
    @SerializedName("shareTxt")
    val shareTxt: String,
    @SerializedName("shareUrl")
    val shareUrl: String,
    @SerializedName("showDesc")
    val showDesc: String,
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
    @SerializedName("titleImg")
    val titleImg: String,
    @SerializedName("voteDown")
    val voteDown: Int,
    @SerializedName("voteUp")
    val voteUp: Int
)

data class VideoDetailPlay(
    @SerializedName("displayName")
    val displayName: String,
    @SerializedName("episodeId")
    val episodeId: Int,
    @SerializedName("episodeName")
    val episodeName: String,
    @SerializedName("external")
    val `external`: Int,
    @SerializedName("langId")
    val langId: Int,
    @SerializedName("playIdCode")
    val playIdCode: String,
    @SerializedName("resolution")
    val resolution: String,
    @SerializedName("resolutionInt")
    val resolutionInt: Int,
    @SerializedName("seq")
    val seq: Int,
    @SerializedName("size")
    val size: Long,
    @SerializedName("sourceId")
    val sourceId: Int
)