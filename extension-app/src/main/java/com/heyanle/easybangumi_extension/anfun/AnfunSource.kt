package com.heyanle.easybangumi_extension.anfun

import com.heyanle.extension_api.ExtensionIconSource
import com.heyanle.extension_api.ExtensionSource
import org.easybangumi.extension.R
import kotlin.reflect.KClass

/**
 * Created by heyanle on 2024/1/28.
 * https://github.com/heyanLE
 */
class AnfunSource : ExtensionSource(), ExtensionIconSource {

    companion object {
        const val ROOT_URL = "https://www.anfuns.cc"
    }

    override fun getIconResourcesId(): Int? {
        return R.drawable.anfun
    }

    override val sourceKey: String
        get() = "heyanle_Anfun"
    override val describe: String?
        get() = null
    override val label: String
        get() = "AnFuns动漫"
    override val version: String
        get() = "1.2"
    override val versionCode: Int
        get() = 2

    override fun register(): List<KClass<*>> {
        return listOf(
            AnfunDetailedComponent::class,
            AnfunListComponent::class,
            AnfunPageComponent::class,
            AnfunSearchComponent::class
        )
    }
}