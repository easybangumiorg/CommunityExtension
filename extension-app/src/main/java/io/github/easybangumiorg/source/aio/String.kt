package io.github.easybangumiorg.source.aio

import java.net.URLEncoder

fun String.encodeUri() = URLEncoder.encode(this, "UTF-8")