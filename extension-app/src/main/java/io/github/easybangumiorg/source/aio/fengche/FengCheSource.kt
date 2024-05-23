package io.github.easybangumiorg.source.aio.fengche

import com.heyanle.easybangumi4.source_api.Source
import com.heyanle.extension_api.ExtensionIconSource
import org.easybangumi.extension.R
import kotlin.reflect.KClass

class FengCheSource : Source, ExtensionIconSource {
    override val describe: String
        get() = "风车动漫"
    override val key: String
        get() = "wedm.cc"
    override val label: String
        get() = "风车动漫"
    override val version: String
        get() = "2.1"
    override val versionCode: Int
        get() = 5

    override fun getIconResourcesId(): Int = R.drawable.fengche

    override fun register(): List<KClass<*>> = listOf(
        FengChePage::class,
        FengCheDetail::class,
        FengChePlay::class,
        FengCheSearch::class,
        FengChePrefer::class,
        FengCheHostUrlHelper::class
    )
}