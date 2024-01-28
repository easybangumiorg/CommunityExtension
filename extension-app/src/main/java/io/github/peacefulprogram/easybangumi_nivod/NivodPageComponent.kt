package io.github.peacefulprogram.easybangumi_nivod

import com.google.gson.Gson
import com.heyanle.easybangumi4.source_api.Source
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.page.PageComponent
import com.heyanle.easybangumi4.source_api.component.page.SourcePage
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.utils.api.NetworkHelper
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.withResult

import io.github.peacefulprogram.easybangumi_nivod.dto.ChannelRecommendResponse
import kotlinx.coroutines.Dispatchers

class NivodPageComponent(
    private val okhttpHelper: OkhttpHelper
) : PageComponent, ComponentWrapper() {

    val gson = Gson()

    override fun getPages(): List<SourcePage> {
        val channels = listOf(
            Pair("首页", null),
            Pair("电影", 1),
            Pair("动漫", 4),
            Pair("电视剧", 2),
            Pair("综艺", 3),
            Pair("纪录片", 6)
        )
        return channels.map { (channelName, channelId) ->
            SourcePage.Group(channelName, false) {
                withResult(Dispatchers.IO) {
                    getChannelRecommend(channelId = channelId)
                }
            }
        }
    }

    private fun getChannelRecommend(channelId: Int?): List<SourcePage.SingleCartoonPage> {
        val pages = mutableListOf<SourcePage.SingleCartoonPage>()
        var start = 0
        val step = 6
        while (true) {
            val req = NivodRequest(
                "/index/desktop/WEB/3.4",
                body = NoEmptyValueMap(
                    "channel_id" to channelId,
                    "start" to start,
                    "more" to "1"
                )
            )
            val resp = gson.fromJson<ChannelRecommendResponse>(
                okhttpHelper.client.newCall(req).execute().decryptResponseBodyIfCan()
            )
            if (resp.banners.isNotEmpty()) {
                SourcePage.SingleCartoonPage.WithCover("推荐", { 1 }) {
                    withResult {
                        null to resp.banners.filter { it.show != null }.map { banner ->
                            CartoonCoverImpl(
                                id = banner.show!!.showIdCode,
                                source = source.key,
                                url = "${NivodConstants.WEBPAGE_URL}/detail.html?showIdCode=${banner.show.showIdCode}",
                                title = banner.show.showTitle,
                                coverUrl = banner.show.showImg
                            )
                        }
                    }
                }.let { pages.add(it) }
            }
            resp.list.forEach { recommendRow ->
                SourcePage.SingleCartoonPage.WithCover(recommendRow.title, { 1 }) {
                    withResult {
                        null to recommendRow.rows.flatMap { it.cells }.map { video ->
                            CartoonCoverImpl(
                                id = video.show.showIdCode,
                                source = source.key,
                                url = "${NivodConstants.WEBPAGE_URL}/detail.html?showIdCode=${video.show.showIdCode}",
                                title = video.show.showTitle,
                                coverUrl = video.show.showImg
                            )
                        }
                    }
                }.let { pages.add(it) }
            }
            if (resp.more != 1) {
                break
            }
            start += step
        }

        return pages

    }

}