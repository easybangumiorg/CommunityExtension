package com.heyanle.easybangumi_extension

import com.heyanle.easybangumi4.source_api.Source
import com.heyanle.easybangumi4.source_api.SourceFactory


/**
 * Created by HeYanLe on 2023/2/19 23:23.
 * https://github.com/heyanLE
 */
class EasySourceFactory: SourceFactory {

    override fun create(): List<Source> {
        return listOf(
            // 添加你的源
        )
    }
}