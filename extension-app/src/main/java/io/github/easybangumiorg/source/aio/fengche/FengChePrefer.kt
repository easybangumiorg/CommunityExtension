package io.github.easybangumiorg.source.aio.fengche

import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.preference.PreferenceComponent
import com.heyanle.easybangumi4.source_api.component.preference.SourcePreference
import com.heyanle.easybangumi4.source_api.component.search.SearchComponent

/**
 * Created by heyanlin on 2024/5/23.
 */
class FengChePrefer : ComponentWrapper(), PreferenceComponent {

    override fun register(): List<SourcePreference> = listOf(
        SourcePreference.Switch("自动从 wedm.cc 获取网址", "auto_host_url", true),
        SourcePreference.Edit("自定义网址（当自动关闭时才有效）", "BaseUrl", "http://www.fcdm9.com/"),
    )
}