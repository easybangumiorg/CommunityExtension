package io.github.easybangumiorg.source.aio.xigua

import com.heyanle.easybangumi4.source_api.ParserException
import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.play.PlayComponent
import com.heyanle.easybangumi4.source_api.entity.CartoonSummary
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.easybangumi4.source_api.entity.PlayLine
import com.heyanle.easybangumi4.source_api.entity.PlayerInfo
import com.heyanle.easybangumi4.source_api.withResult
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.commonHttpClient
import io.github.easybangumiorg.source.aio.newGetRequest
import kotlinx.coroutines.Dispatchers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class XiguaPlay : PlayComponent, ComponentWrapper() {

    override suspend fun getPlayInfo(
        summary: CartoonSummary,
        playLine: PlayLine,
        episode: Episode
    ): SourceResult<PlayerInfo> {
        val url = "$XiguaBaseUrl/video/${summary.id}/${episode.id}.html"
        return withResult(Dispatchers.IO) {
            val doc = commonHttpClient.newGetRequest {
                url(url)
            }.asDocument()
            val iframe =
                doc.selectFirst("#video_content iframe")
                    ?: throw ParserException("未获取到播放器")
            val iframeSrc = iframe.attr("src")
            if (iframeSrc.isEmpty()) {
                throw ParserException("未获取到播放器链接")
            }
            val vid = iframeSrc.toHttpUrlOrNull()?.queryParameter("vid")
            val videoUrl = if (vid != null) {
                "https://xgct-video.vzcdn.net/$vid/playlist.m3u8"
            } else {
                commonHttpClient.newGetRequest { url(iframeSrc) }.asDocument()
                    .getElementsByTag("source")
                    .firstOrNull()
                    ?.attr("src")
                    ?.takeIf { it.isNotEmpty() }
                    ?: throw RuntimeException("未获取到video source")
            }
            PlayerInfo(
                decodeType = PlayerInfo.DECODE_TYPE_HLS,
                uri = videoUrl
            )
        }
    }

    companion object {
        private const val TAG = "XiguaPlay"
    }
}