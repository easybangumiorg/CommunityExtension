package io.github.easybangumiorg.source.aio

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.nio.charset.Charset

val json = Json {
    ignoreUnknownKeys = true
}


val commonHttpClient = OkHttpClient.Builder()
    .addInterceptor {
        val req = it.request()
        val builder = req.newBuilder()
        if (req.header("user-agent")?.isNotEmpty() != true) {
            builder.header(
                "user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
        }
        it.proceed(builder.build())
    }
    .build()

fun OkHttpClient.newRequest(block: Request.Builder.() -> Unit): Response {
    val req = Request.Builder()
        .apply(block)
        .build()
    return newCall(req).execute()
}

fun OkHttpClient.newGetRequest(block: Request.Builder.() -> Unit): Response {
    return newRequest {
        block()
        get()
    }
}

fun Response.asDocument(): Document {
    return Jsoup.parse(this.bodyString()).apply {
        setBaseUri(request.url.toString())
    }
}

fun Response.bodyString(charset: Charset = Charsets.UTF_8): String {
    if (code != 200) {
        throw RuntimeException("请求${request.url}失败,code: $code")
    }
    return this.use {
        val body = it.body ?: throw RuntimeException("请求${request.url}失败,响应体为空")
        body.string()
    }
}

inline fun <reified T> Response.readJson(): T {
    return json.decodeFromString<T>(this.bodyString())
}

