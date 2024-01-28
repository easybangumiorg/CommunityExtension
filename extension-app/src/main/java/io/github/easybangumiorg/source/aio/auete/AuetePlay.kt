package io.github.easybangumiorg.source.aio.auete

import com.heyanle.easybangumi4.source_api.ParserException
import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.play.PlayComponent
import com.heyanle.easybangumi4.source_api.entity.CartoonSummary
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.easybangumi4.source_api.entity.PlayLine
import com.heyanle.easybangumi4.source_api.entity.PlayerInfo
import io.github.easybangumiorg.source.aio.bodyString
import io.github.easybangumiorg.source.aio.commonHttpClient
import io.github.easybangumiorg.source.aio.newGetRequest
import io.github.easybangumiorg.source.aio.withIoResult
import kotlin.io.encoding.ExperimentalEncodingApi

class AuetePlay : ComponentWrapper(), PlayComponent {
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun getPlayInfo(
        summary: CartoonSummary,
        playLine: PlayLine,
        episode: Episode
    ): SourceResult<PlayerInfo> = withIoResult {
        val html = commonHttpClient.newGetRequest {
            url("$AueteBaseUrl/${summary.id}/${episode.id}.html")
        }.bodyString()
        val idx = html.indexOf(" now")
        var start = -1
        if (idx > 0) {
            for (i in (idx + 4) until html.length) {
                val c = html[i]
                if (c == '"') {
                    if (start == -1) {
                        start = i
                    } else {
                        val videoUrl = if (html.substring(idx, start).contains("base64")) {
                            kotlin.io.encoding.Base64.decode(html.substring(start + 1, i))
                                .toString(Charsets.UTF_8)
                        } else {
                            html.substring(start + 1, i)
                        }
                        return@withIoResult PlayerInfo(
                            uri = videoUrl,
                            decodeType = PlayerInfo.DECODE_TYPE_HLS
                        )
                    }
                }
            }
        }
        throw ParserException("未获取到视频链接")
    }
}