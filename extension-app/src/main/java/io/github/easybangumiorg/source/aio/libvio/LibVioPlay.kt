package io.github.easybangumiorg.source.aio.libvio

import com.google.gson.Gson
import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.play.PlayComponent
import com.heyanle.easybangumi4.source_api.entity.CartoonSummary
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.easybangumi4.source_api.entity.PlayLine
import com.heyanle.easybangumi4.source_api.entity.PlayerInfo
import io.github.easybangumiorg.source.aio.bodyString
import io.github.easybangumiorg.source.aio.decodeUri
import io.github.easybangumiorg.source.aio.encodeUri
import io.github.easybangumiorg.source.aio.newGetRequest
import io.github.easybangumiorg.source.aio.withIoResult
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class LibVioPlay(private val libVioSource: LibVioSource) : ComponentWrapper(), PlayComponent {
    private val playerServerCache = ConcurrentHashMap<String, String>()

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun getPlayInfo(
        summary: CartoonSummary,
        playLine: PlayLine,
        episode: Episode
    ): SourceResult<PlayerInfo> = withIoResult {
        val html = libVioSource.requestWithFunCDNInterceptor {
            url("${LibVioSource.BASE_URL}/play/${episode.id}.html")
            get()
        }.bodyString()
        val keyword = "player_aaaa"
        val startIndex = html.indexOf(keyword)
        if (startIndex == -1) {
            throw RuntimeException("未获取到视频播放信息")
        }
        val configStartIndex = html.indexOf('{', startIndex = startIndex + keyword.length)
        val configJson =
            html.substring(
                configStartIndex,
                html.indexOf('}', startIndex = configStartIndex + 1) + 1
            )
        val cfg = Gson().fromJson<Map<String, Any>>(configJson, Map::class.java)
        val encryptMode = cfg["encrypt"]?.toString() ?: ""
        val url = cfg["url"]?.toString()?.takeIf { it.isNotEmpty() }
            ?: throw RuntimeException("播放信息中无url")
        val nextLink = cfg["link_next"]?.toString()?.encodeUri() ?: ""
        val data = when (encryptMode) {
            "1" -> url.decodeUri()
            "2" -> Base64.decode(url).toString(Charsets.UTF_8).decodeUri()
            else -> url
        }
        val playFrom = cfg["from"]?.toString()?.takeIf { it.isNotEmpty() }
            ?: throw RuntimeException("播放器配置中无from属性")
        var playerServer = playerServerCache[playFrom]
        if (playerServer?.isNotEmpty() != true) {
            val jsContent = libVioSource.requestWithFunCDNInterceptor {
                url("${LibVioSource.BASE_URL}/static/player/${playFrom}.js?v=3.5")
            }.bodyString()
            playerServer = parseServerFromJs(jsContent)
            playerServerCache[playFrom] = playerServer
        }
        val playerHtml =
            libVioSource.httpClient.newGetRequest {
                url("$playerServer?url=$data&next=$nextLink&id=${summary.id}&nid=${cfg["nid"]}")
                header("referer", "${LibVioSource.BASE_URL}/")
            }.bodyString()
        val videoUrl = getStringVariableValue(html = playerHtml, variableName = "urls")
            ?: throw RuntimeException("未获取到视频链接")
        PlayerInfo(uri = videoUrl)
    }


    private fun parseServerFromJs(jsContent: String): String {
        val keyword = " src="
        val idx = jsContent.indexOf(keyword)
        if (idx == -1) {
            throw RuntimeException("js中无src")
        }
        var startIndex = -1
        var endIndex = -1
        for (i in (keyword.length + idx) until jsContent.length) {
            val c = jsContent[i]
            if (c == '"' || c == '\'') {
                if (startIndex == -1) {
                    startIndex = i + 1
                } else {
                    endIndex = i
                    break
                }
            }
        }
        if (startIndex >= endIndex) {
            throw RuntimeException("提前src失败")
        }
        return jsContent.substring(startIndex, endIndex).run {
            val questionIndex = indexOf('?')
            if (questionIndex == -1) {
                this
            } else {
                substring(0, questionIndex)
            }
        }
    }

    private fun getStringVariableValue(html: String, variableName: String): String? {
        val keyword = "var $variableName"
        val index = html.indexOf(keyword)
        if (index == -1) {
            return null
        }
        var startIndex = -1
        var quote = '"'
        var endIndex = -1
        for (i in (index + keyword.length) until html.length) {
            val c = html[i]
            if (startIndex == -1 && (c == '\'' || c == '"')) {
                startIndex = i + 1
                quote = c
                continue
            }
            if (startIndex != -1 && c == quote) {
                endIndex = i
                break
            }
        }
        if (startIndex < endIndex) {
            return html.substring(startIndex, endIndex)
        }
        return null
    }

}