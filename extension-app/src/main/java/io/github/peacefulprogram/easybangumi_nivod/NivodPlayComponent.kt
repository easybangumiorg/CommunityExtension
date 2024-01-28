package io.github.peacefulprogram.easybangumi_nivod

import com.google.gson.Gson
import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.play.PlayComponent
import com.heyanle.easybangumi4.source_api.entity.CartoonSummary
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.easybangumi4.source_api.entity.PlayLine
import com.heyanle.easybangumi4.source_api.entity.PlayerInfo
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.withResult

import io.github.peacefulprogram.easybangumi_nivod.dto.VideoStreamUrlResponse
import kotlinx.coroutines.Dispatchers

class NivodPlayComponent(
    private val okhttpHelper: OkhttpHelper,
) : ComponentWrapper(), PlayComponent {
    val gson = Gson()
    override suspend fun getPlayInfo(
        summary: CartoonSummary,
        playLine: PlayLine,
        episode: Episode
    ): SourceResult<PlayerInfo> {
        return withResult(Dispatchers.IO) {
            val episodeId = episode.id
            val req = NivodRequest(
                "/show/play/info/WEB/3.2",
                body = mapOf(
                    "show_id_code" to summary.id,
                    "play_id_code" to episodeId
                )
            )
            val resp = gson.fromJson<VideoStreamUrlResponse>(
                okhttpHelper.client.newCall(req).execute().decryptResponseBodyIfCan()
            )
            PlayerInfo(
                decodeType = PlayerInfo.DECODE_TYPE_HLS,
                uri = resp.entity.playUrl
            )
        }
    }

}