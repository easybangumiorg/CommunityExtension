package io.github.peacefulprogram.easybangumi_nivod

import com.google.gson.Gson
import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.search.SearchComponent
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.withResult
import io.github.peacefulprogram.easybangumi_nivod.dto.SearchVideoResponse
import kotlinx.coroutines.Dispatchers

class NivodSearchComponent(
    private val okhttpHelper: OkhttpHelper,
) : ComponentWrapper(), SearchComponent {
    private val pageSize = 20
    private val gson = Gson()
    override fun getFirstSearchKey(keyword: String): Int {
        return 0
    }

    override suspend fun search(
        pageKey: Int,
        keyword: String
    ): SourceResult<Pair<Int?, List<CartoonCover>>> {
        return withResult(Dispatchers.IO) {
            val req = NivodRequest(
                "/show/search/WEB/3.2",
                body = mapOf(
                    "keyword" to keyword,
                    "start" to pageKey.toString(),
                    "cat_id" to "1",
                    "keyword_type" to "0"
                )
            )
            val resp = gson.fromJson<SearchVideoResponse>(
                okhttpHelper.client.newCall(req).execute().decryptResponseBodyIfCan()
            )
            val nextPageKey = if (resp.more == 1) pageKey + pageSize else null
            val videos = resp.list.map { video ->
                CartoonCoverImpl(
                    id = video.showIdCode,
                    source = source.key,
                    url = "${NivodConstants.WEBPAGE_URL}/detail.html?showIdCode=${video.showIdCode}",
                    title = video.showTitle,
                    coverUrl = video.showImg,
                    intro = video.episodesTxt
                )
            }
            Pair(nextPageKey, videos)
        }

    }


}