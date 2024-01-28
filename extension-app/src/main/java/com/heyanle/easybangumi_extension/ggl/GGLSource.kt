package com.heyanle.easybangumi_extension.ggl

import com.heyanle.easybangumi4.source_api.Source
import com.heyanle.extension_api.ExtensionIconSource
import com.heyanle.extension_api.ExtensionSource
import org.easybangumi.extension.R
import kotlin.reflect.KClass

/**
 * Created by heyanle on 2024/1/28.
 * https://github.com/heyanLE
 */
class GGLSource : ExtensionSource(), ExtensionIconSource {

    override val describe: String?
        get() = "girigirilove"
    override val label: String
        get() = "girigirilove"
    override val version: String
        get() = "3.0"
    override val versionCode: Int
        get() = 3

    override fun register(): List<KClass<*>> {
        return listOf(
            GGLComponent::class,
            GGLListComponent::class,
            GGLPlayComponent::class,
            GGLDetailedComponent::class
        )
    }

    override fun getIconResourcesId(): Int? {
        return R.drawable.ggl
    }

    override val sourceKey: String
        get() = "heyanle_girigirilove"
}