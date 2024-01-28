package io.github.easybangumiorg.source.aio.auete

import com.heyanle.easybangumi4.source_api.Source
import com.heyanle.extension_api.ExtensionIconSource
import org.easybangumi.extension.R
import kotlin.reflect.KClass

class AueteSource : Source, ExtensionIconSource {
    override val describe: String
        get() = label
    override val key: String
        get() = "auete"
    override val label: String
        get() = "Auete影视"
    override val version: String
        get() = "2.0"
    override val versionCode: Int
        get() = 4

    override fun getIconResourcesId(): Int = R.drawable.auete

    override fun register(): List<KClass<*>> = listOf(
        AuetePage::class,
        AueteDetail::class,
        AueteSearch::class,
        AuetePlay::class
    )
}