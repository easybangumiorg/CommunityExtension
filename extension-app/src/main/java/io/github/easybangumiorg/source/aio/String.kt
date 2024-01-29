package io.github.easybangumiorg.source.aio

import java.net.URLDecoder
import java.net.URLEncoder

fun String.encodeUri(): String = URLEncoder.encode(this, "UTF-8")

fun String.decodeUri(): String = URLDecoder.decode(this, "UTF-8")
