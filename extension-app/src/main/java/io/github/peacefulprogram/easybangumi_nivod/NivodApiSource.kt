package io.github.peacefulprogram.easybangumi_nivod

import com.heyanle.easybangumi4.source_api.Source
import com.heyanle.extension_api.ExtensionIconSource
import com.heyanle.extension_api.ExtensionSource
import org.easybangumi.extension.R
import kotlin.reflect.KClass

class NivodApiSource : Source, ExtensionIconSource {
    override val describe: String
        get() = "nivod.tv,科学上网"
    override val label: String
        get() = "泥视频"
    override val version: String
        get() = "2.0"
    override val versionCode: Int
        get() = 4

    override fun getIconResourcesId(): Int = R.drawable.nivod

    override fun register(): List<KClass<*>> {
        return listOf(
            NivodPageComponent::class,
            NivodSearchComponent::class,
            NivodDetailComponent::class,
            NivodPlayComponent::class
        )
    }

    override val key: String
        get() = "io.github.peacefulprogram.easybangumi_nivod-io.github.peacefulprogram.easybangumi_nivod"


}