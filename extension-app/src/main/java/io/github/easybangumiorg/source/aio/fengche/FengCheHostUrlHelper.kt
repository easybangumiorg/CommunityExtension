package io.github.easybangumiorg.source.aio.fengche

import com.heyanle.easybangumi4.source_api.utils.api.PreferenceHelper
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.commonHttpClient
import io.github.easybangumiorg.source.aio.newGetRequest

/**
 * Created by heyanlin on 2024/5/23.
 */
class FengCheHostUrlHelper(
    private val preferenceHelper: PreferenceHelper
) {

    private val autoHostUrl: Boolean
        get() = preferenceHelper.get("auto_host_url", "false") == "true"

    private val diyUrl: String
        get() = preferenceHelper.get("BaseUrl", "http://www.fcdm9.com/")

    private val urlPageUrl = "https://wedm.cc/"

    private val fengcheBaseUrlAutoUrl: String by lazy {
        val doc = commonHttpClient.newGetRequest {
            url(urlPageUrl)
        }.asDocument()

        val website = doc.selectFirst(".main .speedlist li a i")?.text()?.trim()?.let { text ->
            text
        }
        return@lazy if (website?.isNotBlank() == true) {
            "https://$website"
        } else {
            diyUrl
        }
    }

    val fengcheBaseUrl: String
        get() = (if(autoHostUrl) fengcheBaseUrlAutoUrl else diyUrl).let {
            if (it.endsWith("/")){
                it.substring(0, it.length - 1)
            }else{
                it
            }
        }




}