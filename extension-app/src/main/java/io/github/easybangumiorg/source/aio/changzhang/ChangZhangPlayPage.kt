package io.github.easybangumiorg.source.aio.changzhang

import com.google.gson.Gson
import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.play.PlayComponent
import com.heyanle.easybangumi4.source_api.entity.CartoonSummary
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.easybangumi4.source_api.entity.PlayLine
import com.heyanle.easybangumi4.source_api.entity.PlayerInfo
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import io.github.easybangumiorg.source.aio.bodyString
import io.github.easybangumiorg.source.aio.newGetRequest
import io.github.easybangumiorg.source.aio.withIoResult
import org.jsoup.Jsoup
import java.util.Stack
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.ExperimentalEncodingApi

class ChangZhangPlayPage(private val okhttpHelper: OkhttpHelper):PlayComponent,ComponentWrapper() {
    override suspend fun getPlayInfo(
        summary: CartoonSummary,
        playLine: PlayLine,
        episode: Episode
    ): SourceResult<PlayerInfo> = withIoResult{
        val html = okhttpHelper.cloudflareClient.newGetRequest {
            url("${ChangZhangSource.BASE_URL}/v_play/${episode.id}.html")
        }.bodyString()
        val videoUrl = getVideoPlainUrlFromHtml(html)
        PlayerInfo(uri = videoUrl, decodeType = if (videoUrl.contains("m3u8")) PlayerInfo.DECODE_TYPE_HLS else PlayerInfo.DECODE_TYPE_OTHER)
    }


    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun getVideoPlainUrlFromHtml(html: String): String {
        val iframeSrc = Jsoup.parse(html).selectFirst(".videoplay > iframe")?.attr("src") ?: ""
        if (iframeSrc.isNotEmpty()) {
            return getVideoUrlFromIframe(iframeSrc)
        }
        val variableName = extractVariableName(html)
        val source = extractVariableValue(html, variableName)
        val (key, iv) = extractKeyAndIv(html)

        val code = with(Cipher.getInstance("AES/CBC/PKCS5Padding")) {
            init(
                Cipher.DECRYPT_MODE, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"),
                IvParameterSpec(iv.toByteArray(Charsets.UTF_8))
            )
            doFinal(kotlin.io.encoding.Base64.decode(source)).toString(Charsets.UTF_8)
        }
        val urlIndex = code.indexOf("url")
        val startIndex = code.indexOf(':', startIndex = urlIndex + 3) + 1
        var quoteStartIndex = -1
        for (i in startIndex until code.length) {
            val c = code[i]
            if (c == '\'' || c == '"') {
                if (quoteStartIndex == -1) {
                    quoteStartIndex = i
                } else {
                    return code.substring(quoteStartIndex + 1, i)
                }
            }
        }
        throw RuntimeException()
    }

    private fun getStringVariableValue(html: String, variableName: String): String? {
        val keyword = " $variableName"
        val idx = html.indexOf(keyword)
        if (idx == -1) {
            return null
        }
        var start = -1
        var quote = '"'
        for (i in (idx + keyword.length) until html.length) {
            val c = html[i]
            if (start == -1 && (c == '\'' || c == '"')) {
                start = i
                quote = c
                continue
            }
            if (start > 0 && c == quote) {
                return html.substring(start + 1, i)
            }
        }
        return null

    }

    private suspend fun getVideoUrlFromIframe(iframeSrc: String): String {
        val resp = okhttpHelper.client.newGetRequest {
            url(iframeSrc)
            header("referer", "https://www.czzy88.com/")
            header("sec-fetch-dest", "iframe")
            header("Sec-Fetch-Mode", "navigate")
            header("Sec-Fetch-Site", "cross-site")
        }
        val html = resp.body!!.string()
        val keyword = "\"data\""
        val idx = html.indexOf(keyword)
        if (idx == -1) {
            return getVideoUrlOfPlayerAndRand(html)
        }
        val startIndex = html.indexOf('"', startIndex = idx + keyword.length)
        var end = -1
        for (i in (startIndex + 1) until html.length) {
            if (html[i] == '"') {
                end = i;
                break
            }
        }
        if (end <= startIndex) {
            throw RuntimeException("未获取到data值")
        }
        val dataValue = html.substring(startIndex + 1, end)
        val newStr = dataValue.reversed().chunked(2)
            .map { it.toInt(16).toChar() }
            .joinToString(separator = "")
        val splitIndex = (newStr.length - 7) / 2
        return newStr.substring(0, splitIndex) + newStr.substring(splitIndex + 0x7)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun getVideoUrlOfPlayerAndRand(html: String): String {
        val rand = getStringVariableValue(html, "rand") ?: throw RuntimeException("未找到rand变量")
        val player =
            getStringVariableValue(html, "player") ?: throw RuntimeException("未找到player变量")
        val key = "VFBTzdujpR9FWBhe"
        val config = with(Cipher.getInstance("AES/CBC/PKCS5Padding")) {
            init(
                Cipher.DECRYPT_MODE, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"),
                IvParameterSpec(rand.toByteArray(Charsets.UTF_8))
            )
            doFinal(kotlin.io.encoding.Base64.decode(player)).toString(Charsets.UTF_8)
        }
        val map = Gson().fromJson<Map<String, Any>>(config, Map::class.java)
        return map["url"]?.let { it as String } ?: throw RuntimeException("未获取到url")
    }

    private fun extractKeyAndIv(html: String): Pair<String, String> {
        var start = 0
        val keyAndIv = mutableListOf<String>()
        val keyword = "Utf8.parse("
        while (start < html.length) {
            val idx = html.indexOf(keyword, startIndex = start)
            if (idx == -1) {
                break
            }
            val valueStart = idx + keyword.length
            for (i in valueStart until html.length) {
                if (html[i] == ')') {
                    val value = html.substring(valueStart, i)
                    keyAndIv.add(value.trim('"', '\''))
                    if (keyAndIv.size == 2) {
                        return keyAndIv[0] to keyAndIv[1]
                    }
                    start = i + 1
                    break
                }
            }
        }
        throw RuntimeException("未获取到key和iv")
    }

    private fun extractVariableValue(html: String, name: String): String {
        val idx = html.indexOf(" $name")
        if (idx == -1) {
            throw RuntimeException()
        }
        var quoteStart = -1
        for (i in (idx + name.length + 1) until html.length) {
            val c = html[i]
            if (c == '\'' || c == '"') {
                if (quoteStart == -1) {
                    quoteStart = i
                } else {
                    return html.substring(quoteStart + 1, i)
                }
            }
        }
        throw RuntimeException()
    }

    private fun extractVariableName(html: String): String {
        val index = html.indexOf("eval(")
        if (index == -1) {
            throw RuntimeException("未找到eval函数调用")
        }
        val bracketStack = Stack<Int>()
        for (i in (index + 4) until html.length) {
            val c = html[i]
            if (c == '(') {
                bracketStack.add(i)
            } else if (c == ')') {
                return html.substring(bracketStack.peek() + 1, i)
            }
        }
        throw RuntimeException("数据不存在")
    }

}