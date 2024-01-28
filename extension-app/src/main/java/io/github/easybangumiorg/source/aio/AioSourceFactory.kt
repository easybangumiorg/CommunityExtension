package io.github.easybangumiorg.source.aio

import com.heyanle.easybangumi4.source_api.Source
import com.heyanle.easybangumi4.source_api.SourceFactory
import io.github.easybangumiorg.source.aio.auete.AueteSource
import io.github.easybangumiorg.source.aio.fengche.FengCheSource
import io.github.easybangumiorg.source.aio.xigua.XiguaSource

class AioSourceFactory : SourceFactory {
    override fun create(): List<Source> = listOf(XiguaSource(), FengCheSource(), AueteSource())
}