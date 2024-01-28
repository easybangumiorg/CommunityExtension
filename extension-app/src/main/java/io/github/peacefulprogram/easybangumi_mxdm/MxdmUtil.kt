package io.github.peacefulprogram.easybangumi_mxdm

import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object MxdmUtil {
    val BASE_URL = "http://www.mxdm9.com"
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
            .url(actualUrl)
            .get()
            .build()
        return okhttpHelper.client.newCall(req).execute().body?.string() ?: throw RuntimeException(
            "响应为空:$url"
        )
    }


    fun hasNextPage(document: Document): Boolean {
        val page = document.getElementById("page") ?: return false
        val currentPageIndex = page.children().indexOfFirst { it.hasClass("page-current") }
        return currentPageIndex != -1 && currentPageIndex < page.childrenSize() - 3
    }

    fun extractImageSrc(imageElement: Element): String {
        var img = imageElement.dataset()["src"] ?: ""
        if (img.isEmpty() && imageElement.hasAttr("src")) {
            img = imageElement.attr("src")
        }
        return img
    }
}