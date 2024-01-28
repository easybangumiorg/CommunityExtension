package com.heyanle.easybangumi_extension.anim

import com.google.gson.JsonParser
import com.heyanle.easybangumi4.source_api.utils.api.NetworkHelper
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.utils.core.network.GET
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup


/**
 * Created by HeYanLe on 2023/7/16 16:56.
 * https://github.com/heyanLE
 */
object DataSource {

    private var allData: List<AnimOneInfo>? = null

    suspend fun getData(
        refresh: Boolean,
        okhttpHelper: OkhttpHelper,
    ):List<AnimOneInfo> {
        return withContext(Dispatchers.IO){
            if(allData == null || refresh){
                val rs = arrayListOf<AnimOneInfo>()
                runCatching {
                    val url = "https://d1zquzjgwo9yb.cloudfront.net/?_="
                    val res = okhttpHelper.client.newCall(GET(url)).execute().body?.string()?:""
                    val jsonElement = JsonParser.parseString(res).asJsonArray
                    rs.addAll(jsonElement.map {
                        val d = it.asJsonArray
                        AnimOneInfo(
                            id = d.get(0).asLong,
                            name = Jsoup.parse(d.get(1).asString).text(),
                            intro = d.get(2).asString,
                            year = d.get(3).asString,
                            season = d.get(4).asString,
                            translator = d.get(5).asString,
                        )
                    })
                }.onFailure {
                    it.printStackTrace()
                }
                allData = rs
                rs
            }else{
                allData?: emptyList()
            }
        }
    }

}