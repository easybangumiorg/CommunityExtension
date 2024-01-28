package io.github.easybangumiorg.source.aio.xigua

import com.heyanle.easybangumi4.source_api.Source
import com.heyanle.extension_api.ExtensionIconSource
import org.easybangumi.extension.R
import kotlin.reflect.KClass

class XiguaSource : Source, ExtensionIconSource {
    override val describe: String
        get() = "西瓜卡通"
    override val key: String
        get() = "xgcartoon"
    override val label: String
        get() = "西瓜卡通"
    override val version: String
        get() = "2.0"
    override val versionCode: Int
        get() = 4

    override fun getIconResourcesId(): Int = R.drawable.xigua


    override fun register(): List<KClass<*>> {
        return listOf(
            XiGuaPage::class,
            XiguaDetail::class,
            XiguaPlay::class,
            XiguaSearch::class
        )
    }
}