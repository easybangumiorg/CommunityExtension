package io.github.easybangumiorg.source.aio.xigua

const val XiguaBaseUrl = "https://cn.xgcartoon.com"

fun String.extractXiguaIdFromUrl() = this.substring(this.lastIndexOf('/') + 1)
