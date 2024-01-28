package io.github.peacefulprogram.easybangumi_mikudm

import com.heyanle.easybangumi4.source_api.Source
import com.heyanle.extension_api.ExtensionIconSource
import com.heyanle.extension_api.ExtensionSource
import org.easybangumi.extension.R
import kotlin.reflect.KClass

class MikudmApiSource : Source, ExtensionIconSource {
    override val describe: String
        get() = label
    override val label: String
        get() = "异世界动漫"
    override val version: String
        get() = "2.0"
    override val versionCode: Int
        get() = 2

    override fun getIconResourcesId(): Int = R.drawable.mikudm
    override val key: String
        get() = "io.github.peacefulprogram.easybangumi_mikudm-io.github.peacefulprogram.easybangumi_mikudm"

    override fun register(): List<KClass<*>> {
        return listOf(
            MikudmPageComponent::class,
            MikudmSearchComponent::class,
            MikudmDetailComponent::class,
            MikudmPlayComponent::class,
        )
    }
}