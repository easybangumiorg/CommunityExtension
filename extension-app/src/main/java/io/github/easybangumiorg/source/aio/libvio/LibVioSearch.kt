package io.github.easybangumiorg.source.aio.libvio

import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.search.SearchComponent
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.encodeUri
import io.github.easybangumiorg.source.aio.withIoResult

class LibVioSearch(
    private val libVioSource: LibVioSource
) : ComponentWrapper(), SearchComponent {
    override fun getFirstSearchKey(keyword: String): Int = 1

    override suspend fun search(
        pageKey: Int,
        keyword: String
    ): SourceResult<Pair<Int?, List<CartoonCover>>> = withIoResult {
        val doc = libVioSource.requestWithFunCDNInterceptor {
            url("${LibVioSource.BASE_URL}/search/${keyword.encodeUri()}----------$pageKey---.html")
        }.asDocument()
        libVioSource.parseVideoPage(doc, pageKey)
    }
}