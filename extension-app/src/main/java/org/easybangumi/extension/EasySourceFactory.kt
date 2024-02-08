package org.easybangumi.extension

import com.heyanle.easybangumi4.source_api.Source
import com.heyanle.easybangumi4.source_api.SourceFactory
import com.heyanle.easybangumi_extension.anfun.AnfunSource
import com.heyanle.easybangumi_extension.anim.AnimOneSource
import com.heyanle.easybangumi_extension.ggl.GGLSource
import io.github.easybangumiorg.source.aio.auete.AueteSource
import io.github.easybangumiorg.source.aio.changzhang.ChangZhangSource
import io.github.easybangumiorg.source.aio.fengche.FengCheSource
import io.github.easybangumiorg.source.aio.libvio.LibVioSource
import io.github.easybangumiorg.source.aio.xigua.XiguaSource
import io.github.peacefulprogram.easybangumi_mikudm.MikudmApiSource
import io.github.peacefulprogram.easybangumi_mxdm.MxdmApiSource
import io.github.peacefulprogram.easybangumi_nivod.NivodApiSource


/**
 * Created by HeYanLe on 2023/2/19 23:23.
 * https://github.com/heyanLE
 */
class EasySourceFactory: SourceFactory {

    override fun create(): List<Source> {
        return listOf(
            AnimOneSource(),
            LibVioSource(),
            AueteSource(),
            FengCheSource(),
            XiguaSource(),
            MikudmApiSource(),
            MxdmApiSource(),
            NivodApiSource(),
            GGLSource(),
            AnfunSource(),
            ChangZhangSource(),
        )
    }
}