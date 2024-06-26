package io.github.peacefulprogram.easybangumi_mikudm

import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.utils.api.PreferenceHelper
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class MikudmUtil(
    private val preferenceHelper: PreferenceHelper
) {
    val BASE_URL:String
        get() = preferenceHelper.get("BaseUrl", "https://www.dmmiku.com").let {
            if (it.endsWith("/")){
                it.substring(0, it.length - 1)
            }else{
                it
            }
        }

    val BASE_M3U8_URL:String
        get() = preferenceHelper.get("BaseM3u8Url", "https://bf.mmiku.net").let {
            if (it.endsWith("/")){
                it.substring(0, it.length - 1)
            }else{
                it
            }
        }
    val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36"

    fun getDocument(okhttpHelper: OkhttpHelper, url: String): String {
        val actualUrl = if (url.startsWith("http")) {
            url
        } else {
            BASE_URL + url
        }
        val req = Request.Builder()
            .header("user-agent", USER_AGENT)
            .header("referer", "$BASE_URL/")
            .url(actualUrl)
            .get()
            .build()
        return okhttpHelper.client.newCall(req).execute().body?.string() ?: throw RuntimeException(
            "响应为空:$url"
        )
    }


    fun hasNextPage(document: Document): Boolean {
        val page = document.selectFirst(".page") ?: return false
        val currentIndex = page.children().indexOfFirst { it.hasClass("active") }
        return page.child(currentIndex + 1).getElementsByTag("a").attr("href") != page.child(
            currentIndex
        ).getElementsByTag("a").attr("href")
    }

    fun extractImageSrc(imageElement: Element): String {
        val img = imageElement.dataset()["original"] ?: ""
        if (img.isEmpty()) {
            return img
        }
        if (img.startsWith("http")) {
            return img
        }
        return BASE_URL + img
    }
}