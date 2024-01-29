package io.github.easybangumiorg.source.aio.libvio

import android.content.Context
import android.os.Build
import android.os.Handler
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.heyanle.easybangumi4.source_api.Source
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.utils.api.StringHelper
import com.heyanle.easybangumi4.source_api.utils.core.setDefaultSettings
import com.heyanle.extension_api.BuildConfig
import com.heyanle.extension_api.ExtensionIconSource
import com.heyanle.extension_api.ExtensionSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.easybangumi.extension.R
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

class LibVioSource : Source,
    ExtensionIconSource {

    override val describe: String
        get() = label
    override val label: String
        get() = "LIBVIO"
    override val key: String
        get() = "libvio"
    override val version: String
        get() = "1.0"
    override val versionCode: Int
        get() = 1

    override fun getIconResourcesId(): Int = R.drawable.libvio

    override fun register(): List<KClass<*>> =
        listOf(LibVioPage::class, LibVioPlay::class, LibVioSearch::class, LibVioDetail::class)

    private lateinit var context: Context

    lateinit var httpClient: OkHttpClient
        private set

    private var cookieManager = CookieManager.getInstance()

    private lateinit var executor: Executor

    private lateinit var stringHelper: StringHelper

    fun init(
        context: Context,
        okhttpHelper: OkhttpHelper,
        stringHelper: StringHelper
    ) {
        this.context = context
        this.stringHelper = stringHelper
        httpClient = okhttpHelper.client
        executor = if (Build.VERSION.SDK_INT >= 28) {
            context.mainExecutor
        } else {
            val handler = Handler(context.mainLooper)
            Executor { command ->
                if (!handler.post(command)) {
                    throw RejectedExecutionException("$handler is shutting down")
                }
            }
        }
    }

    private suspend fun CookieManager.removeAllCookie(url: String) {
        val cookieStr = getCookie(url)?.takeIf { it.isNotEmpty() } ?: return
        val httpUrl = url.toHttpUrl()
        val host = url.toHttpUrl().host
        val cookieNames = cookieStr.split(";").asSequence()
            .map { Cookie.parse(httpUrl, it)?.name }
            .filterNotNull()
            .toList()
        if (cookieNames.isEmpty()) {
            return
        }
        val defList = List(cookieNames.size) { CompletableDeferred<Pair<String, Boolean>>() }
        withContext(Dispatchers.Main) {
            cookieNames.forEachIndexed { index, cookieName ->
                setCookie(host, "$cookieName=; path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT") {
                    defList[index].complete(cookieName to it)
                }
            }
            flush()
        }
        val removeSuccess = defList.awaitAll()
        removeSuccess.forEach { (name, success) ->
            Log.d(TAG, "removeAllCookie: $name success: $success")
        }
    }

    suspend fun requestWithFunCDNInterceptor(block: Request.Builder.() -> Unit): Response {
        val req = Request.Builder().apply(block).build()
        val resp = httpClient.executeRequestSuspend(req)
        if (resp.code != 512) {
            return resp
        }
        resp.close()
        stringHelper.moeSnackBar("正在进行FunCDN检测,请耐心等待")
        val requestUrl = resp.request.url.toString()
        Log.d(
            TAG,
            "requestWithFunCDNInterceptor: oldCookie:${cookieManager.getCookie(requestUrl)}"
        )
        cookieManager.removeAllCookie(BASE_URL)
        Log.d(
            TAG,
            "requestWithFunCDNInterceptor: oldCookie:${cookieManager.getCookie(requestUrl)}"
        )
        val cookieName = "_funcdn_token"
        val cookieDef = CompletableDeferred<Unit>()
        val webView: WebView = withContext(Dispatchers.Main) {
            WebView(context).apply {
                setDefaultSettings()
                settings.userAgentString = resp.request.header("user-agent")
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String) {
                        Log.d(TAG, "onPageFinished: $url")
                        if (url != requestUrl) {
                            return
                        }
                        val cookieStr = cookieManager.getCookie(requestUrl)
                        Log.d(TAG, "onPageFinished: $cookieStr")
                        if (cookieStr?.isNotEmpty() == true) {
                            val found = cookieStr.split(";")
                                .any {
                                    val ck = Cookie.parse(resp.request.url, it)
                                    if (BuildConfig.DEBUG) {
                                        Log.d(TAG, "webViewCookie: $it")
                                    }
                                    ck != null && ck.name == cookieName && ck.value.isNotEmpty()
                                }
                            if (found) {
                                cookieDef.complete(Unit)
                            }
                        }
                    }
                }
                loadUrl(requestUrl)
            }
        }
        try {
            withTimeout(60.seconds) {
                cookieDef.await()
            }
        } finally {
            executor.execute {
                with(webView) {
                    stopLoading()
                    destroy()
                }
            }
        }
        return httpClient.executeRequestSuspend(req)
    }

    private suspend fun OkHttpClient.executeRequestSuspend(req: Request): Response {
        val def = CompletableDeferred<Result<Response>>()
        newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                def.complete(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                def.complete(Result.success(response))
            }
        })
        return def.await().getOrThrow()
    }


    fun parseLibVioVideo(videoEl: Element): CartoonCover {
        val linkEl =
            videoEl.selectFirst(".stui-vodlist__thumb") ?: throw RuntimeException("未找到视频图片")
        val image = linkEl.dataset()["original"] ?: ""
        val url = linkEl.absUrl("href")
        val episode = linkEl.selectFirst(".pic-text")?.text() ?: ""
        val title =
            videoEl.selectFirst(".stui-vodlist__detail > .title")?.text()?.trim()
                ?: throw RuntimeException(
                    "未找到视频标题"
                )
        return CartoonCoverImpl(
            id = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.')),
            url = url,
            source = key,
            title = title,
            intro = episode,
            coverUrl = image
        )
    }

    fun parseVideoPage(doc: Document, page: Int): Pair<Int?, List<CartoonCover>> {
        val videos = doc.select(".stui-vodlist .stui-vodlist__box").map { parseLibVioVideo(it) }
        val next = if (haveNextPage(doc)) page + 1 else null
        return next to videos
    }

    private fun haveNextPage(doc: Document): Boolean {
        val pageEls = doc.select(".stui-pannel__ft > .stui-page__item > li")
        val currentIndex = pageEls.indexOfFirst { it.hasClass("active") }
        return currentIndex != -1 && currentIndex < pageEls.size - 4
    }

    companion object {

        private const val TAG = "LibVioSource"

        const val BASE_URL = "https://www.libvio.vip"
    }
}