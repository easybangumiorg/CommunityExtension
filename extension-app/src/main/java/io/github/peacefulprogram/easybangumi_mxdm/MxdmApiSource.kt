package io.github.peacefulprogram.easybangumi_mxdm

import com.heyanle.easybangumi4.source_api.Source
import com.heyanle.extension_api.ExtensionIconSource
import com.heyanle.extension_api.ExtensionSource
import org.easybangumi.extension.R
import kotlin.reflect.KClass

class MxdmApiSource : Source, ExtensionIconSource {
    override val describe: String
        get() = label
    override val label: String
        get() = "MX动漫"
    override val version: String
        get() = "1.1"
    override val versionCode: Int
        get() = 2

    override fun getIconResourcesId(): Int = R.drawable.mxdm

    override val key: String
        get() = "io.github.peacefulprogram.easybangumi_mxdm-io.github.peacefulprogram.easybangumi_mxdm"

    override fun register(): List<KClass<*>> {
        return listOf(
            MxdmPageComponent::class,
            MxdmSearchComponent::class,
            MxdmDetailComponent::class,
            MxdmPlayComponent::class,
            MxdmPreferenceComponent::class,
            MxdmUtil::class
        )
    }
}