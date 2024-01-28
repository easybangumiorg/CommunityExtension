package io.github.easybangumiorg.source.aio



import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.withResult
import kotlinx.coroutines.Dispatchers

fun <T, R> SourceResult<T>.map(convert: (T) -> R): SourceResult<R> {
    return when (this) {
        is SourceResult.Complete -> SourceResult.Complete(convert(data))
        is SourceResult.Error -> SourceResult.Error(throwable, isParserError)
    }
}

suspend fun <T> withIoResult(block: suspend () -> T) = withResult(Dispatchers.IO, block)