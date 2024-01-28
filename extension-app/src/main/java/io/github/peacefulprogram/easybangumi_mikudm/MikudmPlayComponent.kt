package io.github.peacefulprogram.easybangumi_mikudm

import android.util.Base64
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
import kotlinx.coroutines.Dispatchers
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class MikudmPlayComponent(
    private val okhttpHelper: OkhttpHelper
) : ComponentWrapper(), PlayComponent {
    override suspend fun getPlayInfo(
        summary: CartoonSummary,
        playLine: PlayLine,
        episode: Episode,
    ): SourceResult<PlayerInfo> = withResult(Dispatchers.IO) {
        val html =
            MikudmUtil.getDocument(okhttpHelper, "/index.php/vod/play/id/${summary.id}/sid/${playLine.id}/nid/${(episode.id.toIntOrNull() ?: episode.order) + 1}.html")
        val newHtml =
            MikudmUtil.getDocument(
                okhttpHelper, "https://bf.sbdm.cc/m3u8.php?url=" + extractPlayerParam(
                    html
                )
            )
        val leToken = extractLeToken(newHtml)
        val encryptedUrl = extractEncryptedUrl(newHtml)

        val plainUrl = Cipher.getInstance("AES/CBC/PKCS5Padding").run {
            init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec("A42EAC0C2B408472".toByteArray(), "AES"),
                IvParameterSpec(leToken.toByteArray(Charsets.UTF_8))
            )
            doFinal(Base64.decode(encryptedUrl, Base64.DEFAULT))
        }.toString(Charsets.UTF_8)
        PlayerInfo(uri = plainUrl, decodeType = PlayerInfo.DECODE_TYPE_HLS).apply {
            header = mapOf("referer" to "https://bf.sbdm.cc/")
        }
    }

    private fun extractPlayerParam(html: String): String {
        val startIndex = html.indexOf("player_aaaa")
        val startBracketIndex = html.indexOf('{', startIndex + 1)
        var endIndex = -1
        var bracketCount = 0
        for (i in (startBracketIndex + 1)..<html.length) {
            val char = html[i]
            if (char == '{') {
                bracketCount++
            } else if (char == '}') {
                if (bracketCount == 0) {
                    endIndex = i
                    break
                }
                bracketCount--
            }
        }
        if (endIndex <= startIndex) {
            throw RuntimeException("未找到播放信息")
        }
        return Gson().fromJson<Map<String, String>>(
            html.substring(startBracketIndex, endIndex + 1),
            Map::class.java
        )["url"] ?: throw RuntimeException("未获取到播放信息")
    }

    fun extractEncryptedUrl(html: String): String {
        val key = "getVideoInfo("
        val i1 = html.indexOf(key)
        val i2 = html.indexOf('"', i1 + key.length)
        val i3 = html.indexOf('"', i2 + 1)
        return html.substring(i2 + 1, i3)
    }

    fun extractLeToken(html: String): String {
        val i1 = html.indexOf("le_token")
        val i2 = html.indexOf('"', i1)
        val i3 = html.indexOf('"', i2 + 1)
        return html.substring(i2 + 1, i3)
    }
}