package io.github.peacefulprogram.easybangumi_nivod

import com.google.gson.Gson
import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.detailed.DetailedComponent
import com.heyanle.easybangumi4.source_api.entity.Cartoon
import com.heyanle.easybangumi4.source_api.entity.CartoonImpl
import com.heyanle.easybangumi4.source_api.entity.CartoonSummary
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.easybangumi4.source_api.entity.PlayLine
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.withResult
import io.github.peacefulprogram.easybangumi_nivod.dto.VideoDetailResponse
import kotlinx.coroutines.Dispatchers

class NivodDetailComponent(
    private val okhttpHelper: OkhttpHelper
) : ComponentWrapper(),
    DetailedComponent {
    val gson = Gson()
    override suspend fun getAll(summary: CartoonSummary): SourceResult<Pair<Cartoon, List<PlayLine>>> {
        return withResult(Dispatchers.IO) {
            val detail = getVideoDetail(summary.id)
            Pair(detail.toCartoon(), getVideoDetail(summary.id).toPlayLine())
        }
    }

    override suspend fun getDetailed(summary: CartoonSummary): SourceResult<Cartoon> {
        return withResult(Dispatchers.IO) {
            getVideoDetail(summary.id).toCartoon()
        }
    }

    override suspend fun getPlayLine(summary: CartoonSummary): SourceResult<List<PlayLine>> {
        return withResult(Dispatchers.IO) {
            getVideoDetail(summary.id).toPlayLine()
        }
    }

    private fun VideoDetailResponse.toPlayLine(): List<PlayLine> {
        val ids = mutableListOf<String>()
        val names = arrayListOf<Episode>()

        entity.plays.forEachIndexed { i, it ->
            ids.add(it.playIdCode)
            names.add(Episode(it.playIdCode, it.episodeName, it.seq))
        }
        return DetailedComponent.NonPlayLine(
            PlayLine(
                "播放列表",
                "泥视频",
                episode = names
            )
        )
    }


    private fun getVideoDetail(id: String): VideoDetailResponse {
        val req = NivodRequest(
            "/show/detail/WEB/3.2",
            body = mapOf("show_id_code" to id)
        )
        return gson.fromJson(
            okhttpHelper.client.newCall(req).execute().decryptResponseBodyIfCan()
        )
    }

    private fun VideoDetailResponse.toCartoon(): Cartoon = CartoonImpl(
        id = this.entity.showIdCode,
        source = source.key,
        url = "${NivodConstants.WEBPAGE_URL}/detail.html?showIdCode=${entity.showIdCode}",
        title = this.entity.showTitle,
        coverUrl = this.entity.showImg,
        description = this.entity.showDesc,
        genre = listOf(
            entity.showTypeName,
            entity.postYear.toString(),
            entity.episodesUpdateDesc
        ).filter { it.isNotBlank() }.joinToString(", ")
    )

}