package com.heyanle.easybangumi_extension.anim

import com.heyanle.easybangumi4.source_api.Source
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.entity.CartoonImpl


/**
 * Created by HeYanLe on 2023/7/16 16:56.
 * https://github.com/heyanLE
 */
data class AnimOneInfo(
    val id: Long,
    val name: String,
    val intro: String,
    val year: String,
    val season: String,
    val translator: String,
){

    fun toCartoon(source: Source) = CartoonImpl(
        id = id.toString(),
        source = source.key,
        url = source.describe?:"",
        title = name,
        genre = "${year}, ${season}, $translator",
        intro = intro,
    )

    fun toCartoonCover(source: Source) = CartoonCoverImpl(
        id = id.toString(),
        source = source.key,
        url = source.describe?:"",
        title = name
    )
}