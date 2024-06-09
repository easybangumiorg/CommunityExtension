package io.github.peacefulprogram.easybangumi_mikudm

import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.preference.PreferenceComponent
import com.heyanle.easybangumi4.source_api.component.preference.SourcePreference

/**
 * Created by heyanle on 2024/6/9.
 * https://github.com/heyanLE
 */
class MikudmPreferenceComponent : ComponentWrapper(), PreferenceComponent {

    override fun register(): List<SourcePreference> = listOf(
        SourcePreference.Edit("异世界动漫网址", "BaseUrl", "https://www.dmmiku.com"),
        SourcePreference.Edit("异世界动漫视频 M3U8 网址", "BaseM3u8Url", "https://bf.mmiku.net"),
    )

    override fun needMigrate(oldVersionCode: Int): Boolean {
        return false
    }
}