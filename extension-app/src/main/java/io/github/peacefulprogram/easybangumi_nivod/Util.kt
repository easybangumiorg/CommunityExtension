package io.github.peacefulprogram.easybangumi_nivod

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

fun Response.decryptResponseBodyIfCan(): String {
    if (code == 403) {
        throw RuntimeException("请求被禁止,请科学上网后重试")
    }
    val encryptedText = body?.string() ?: throw RuntimeException("响应体为空")
    val cipherText = (encryptedText).decodeHexString()
    val key = "diao.com".toByteArray()
    return Cipher.getInstance("DES/ECB/PKCS5Padding").run {
        init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "DES"))
        doFinal(cipherText)
    }.toString(Charsets.UTF_8)
}

private fun String.decodeHexString(): ByteArray =
    this.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

@OptIn(ExperimentalStdlibApi::class)
private fun createSign(queryParams: Map<String, String>, body: Map<String, String>): String {
    val prefixes = arrayOf("__QUERY::", "__BODY::")
    val params = arrayOf(queryParams, body)
    val str = StringBuilder()
    for (i in prefixes.indices) {
        val param = params[i]
        str.append(prefixes[i])
        param.keys.toList().sorted().filter {
            it.isNotEmpty() && param[it]?.isNotEmpty() == true
        }.forEach {
            str.append(it)
            str.append('=')
            str.append(param[it])
            str.append('&')
        }
    }
    str.append("__KEY::2x_Give_it_a_shot")
    return MessageDigest.getInstance("MD5").digest(str.toString().toByteArray(Charsets.UTF_8))
        .toHexString()
}

private fun HttpUrl.Builder.withSign(
    body: Map<String, String> = emptyMap(),
    queryParams: Map<String, String> = emptyMap()
): HttpUrl.Builder {
    val allQueryParams = mutableMapOf(
        "_ts" to System.currentTimeMillis().toString(),
        "app_version" to "1.0",
        "platform" to "3",
        "market_id" to "web_nivod",
        "device_code" to "web",
        "versioncode" to "1",
        "oid" to "8ca275aa5e12ba504b266d4c70d95d77a0c2eac5726198ea"
    ).apply {
        putAll(queryParams)
    }
    allQueryParams.forEach { (name, value) ->
        addQueryParameter(name, value)
    }
    addQueryParameter("sign", createSign(allQueryParams, body = body))
    return this
}


fun NoEmptyValueMap(vararg pairs: Pair<String, Any?>): Map<String, String> {
    val result =
        pairs.asSequence()
            .filter { (k, v) -> k.isNotEmpty() && v != null && (v !is String || v.isNotEmpty()) }
            .map { (k, v) -> k to v.toString() }
            .toList()
            .toTypedArray()
    return mapOf(*result)
}

inline fun <reified T> Gson.fromJson(json: String): T = fromJson(json, object : TypeToken<T>() {})


fun NivodRequest(
    url: String,
    body: Map<String, String> = emptyMap(),
    queryParams: Map<String, String> = emptyMap()
): Request {
    val url = (NivodConstants.BASE_URL + url).toHttpUrl()
        .newBuilder()
        .withSign(body, queryParams)
        .build()
    val formBody = FormBody.Builder()
        .apply {
            body.entries.forEach { (name, value) ->
                add(name, value)
            }
        }.build()
    return Request.Builder()
        .header("referer", NivodConstants.REFERER)
        .header(
            "user-agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36"
        )
        .post(formBody)
        .url(url)
        .build()
}