package com.heyanle.easybangumi_extension.ggl

import android.util.Log
import com.heyanle.easybangumi4.source_api.ParserException
import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.play.PlayComponent
import com.heyanle.easybangumi4.source_api.entity.CartoonSummary
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.easybangumi4.source_api.entity.PlayLine
import com.heyanle.easybangumi4.source_api.entity.PlayerInfo
import com.heyanle.easybangumi4.source_api.utils.api.NetworkHelper
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.utils.api.WebViewHelper
import com.heyanle.easybangumi4.source_api.utils.core.SourceUtils
import com.heyanle.easybangumi4.source_api.withResult
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup

/**
 * Created by heyanle on 2024/1/28.
 * https://github.com/heyanLE
 */
class GGLPlayComponent(
    private val webViewHelper: WebViewHelper,
    private val networkHelper: NetworkHelper,
): ComponentWrapper(), PlayComponent  {

    override suspend fun getPlayInfo(
        summary: CartoonSummary,
        playLine: PlayLine,
        episode: Episode
    ): SourceResult<PlayerInfo> {
        return withResult(Dispatchers.IO) {
            val urlPath = "${if(summary.id.startsWith("GV"))summary.id else "GV${summary.id}"}-${playLine.id}-${episode.id}"
            val url = SourceUtils.urlParser(GGLListComponent.ROOT_URL, "/play${urlPath}/")
            val doc = webViewHelper.getRenderedHtmlCode(url = url, callBackRegex = "https://anime.girigirilove.com/addons/dp/player/index.php?.*", "utf-8", networkHelper.defaultLinuxUA, null, null, 20000L)
            Log.i("GGLPlayComponent", doc)
            val jsoup = Jsoup.parse(doc)
            val src = jsoup.select("tbody td iframe").first()?.attr("src")?:""
            val u = src.split("?").last().split("&").find {
                it.startsWith("url=")
            }?.let {
                it.subSequence(4, it.length)
            }?.toString() ?:""
            if(u.isEmpty()){
                throw ParserException("url 解析失败")
            }
            PlayerInfo(
                decodeType = if(u.endsWith("m3u8")) PlayerInfo.DECODE_TYPE_HLS else PlayerInfo.DECODE_TYPE_OTHER,
                uri = u
            )
        }
    }

}