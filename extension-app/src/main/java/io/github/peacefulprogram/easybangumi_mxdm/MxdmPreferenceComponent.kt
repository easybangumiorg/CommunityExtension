package io.github.peacefulprogram.easybangumi_mxdm

import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.play.PlayComponent
import com.heyanle.easybangumi4.source_api.component.preference.PreferenceComponent
import com.heyanle.easybangumi4.source_api.component.preference.SourcePreference

/**
 * Created by heyanlin on 2024/5/22.
 */
class MxdmPreferenceComponent : ComponentWrapper(), PreferenceComponent {

    override fun register(): List<SourcePreference> = listOf(
        SourcePreference.Edit("MX 动漫网址", "BaseUrl", "https://www.mxdm.tv/"),
    )

    override fun needMigrate(oldVersionCode: Int): Boolean {
        return false
    }
}