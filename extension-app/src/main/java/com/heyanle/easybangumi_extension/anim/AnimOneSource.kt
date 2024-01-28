package com.heyanle.easybangumi_extension.anim


import com.heyanle.easybangumi4.source_api.Source
import com.heyanle.extension_api.ExtensionIconSource
import com.heyanle.extension_api.ExtensionSource
import org.easybangumi.extension.R
import kotlin.reflect.KClass

/**
 * Created by HeYanLe on 2023/6/6 18:25.
 * https://github.com/heyanLE
 */
class AnimOneSource  : Source, ExtensionIconSource {

    override fun getIconResourcesId(): Int? {
        return R.drawable.anim1
    }

    override val key: String
        get() = "com.heyanle.easybangumi_extension.animone-anim_one"
    override val describe: String?
        get() = "https://anime1.me/"
    override val label: String
        get() = "Anim1"
    override val version: String
        get() = "2.0"
    override val versionCode: Int
        get() = 2

    override fun register(): List<KClass<*>> {
        return listOf(
            AnimPageComponent::class
        )
    }
}