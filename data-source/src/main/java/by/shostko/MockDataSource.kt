@file:Suppress("unused")

package by.shostko

import androidx.paging.DataSource
import androidx.paging.PageKeyedDataSource
import androidx.paging.PositionalDataSource
import kotlin.math.max
import kotlin.math.min

object MockDataSource {

    @Suppress("UNCHECKED_CAST")
    fun <V> empty(): DataSource.Factory<Int, V> = EmptyPositionalDataSource.Factory as DataSource.Factory<Int, V>

    @Suppress("UNCHECKED_CAST")
    fun <K, V> emptyContiguous(): DataSource.Factory<K, V> = EmptyPageKeyedDataSource.Factory as DataSource.Factory<K, V>

    fun <V> items(array: Array<V>): DataSource.Factory<Int, V> = if (array.isEmpty()) empty() else PreloadedDataSource(array).cache()

    inline fun <reified V> items(collection: Collection<V>): DataSource.Factory<Int, V> = if (collection.isEmpty()) empty() else items(
        collection.toTypedArray()
    )

    fun <V> pages(
        array: Array<List<V>?>,
        keyOffset: Int = 0,
        initialKey: Int? = null
    ): DataSource.Factory<Int, V> =
        if (array.isEmpty()) empty() else PreloadedPageKeyedDataSource(array, keyOffset, initialKey).cache()

    inline fun <reified V> pages(
        collection: Collection<List<V>?>,
        keyOffset: Int = 0,
        initialKey: Int? = null
    ): DataSource.Factory<Int, V> =
        if (collection.isEmpty()) empty() else pages(
            collection.toTypedArray(),
            keyOffset,
            initialKey
        )

    fun <K, V> pages(
        initial: () -> K,
        page: (K) -> List<V>?,
        prevKey: (K) -> K?,
        nextKey: (K) -> K?
    ): DataSource.Factory<K, V> = PageGeneratorDataSource(initial, page, prevKey, nextKey).cache()

    fun <K, V> pages(
        initial: K,
        page: (K) -> List<V>?,
        prevKey: (K) -> K?,
        nextKey: (K) -> K?
    ): DataSource.Factory<K, V> = PageGeneratorDataSource({ initial }, page, prevKey, nextKey).cache()

    fun <K, V> pages(
        initial: K,
        pages: Map<K, List<V>>,
        prevKey: (K) -> K?,
        nextKey: (K) -> K?
    ): DataSource.Factory<K, V> = if (pages.isEmpty()) emptyContiguous() else PageGeneratorDataSource(
        { initial },
        { pages[it] },
        prevKey,
        nextKey
    ).cache()

    fun <K, V> separatedPages(
        initial: () -> K,
        page: (K) -> List<V>?,
        header: ((K, List<V>?) -> List<V>?)?,
        footer: ((K, List<V>?) -> List<V>?)?,
        prevKey: (K) -> K?,
        nextKey: (K) -> K?
    ): DataSource.Factory<K, V> = SeparatedPageGeneratorDataSource(initial, page, header, footer, prevKey, nextKey).cache()

    fun <K, V> separatedPages(
        initial: K,
        page: (K) -> List<V>?,
        header: ((K, List<V>?) -> List<V>?)?,
        footer: ((K, List<V>?) -> List<V>?)?,
        prevKey: (K) -> K?,
        nextKey: (K) -> K?
    ): DataSource.Factory<K, V> = SeparatedPageGeneratorDataSource({ initial }, page, header, footer, prevKey, nextKey).cache()

    fun <K, V> separatedPages(
        initial: K,
        pages: Map<K, List<V>>,
        header: ((K, List<V>?) -> List<V>?)?,
        footer: ((K, List<V>?) -> List<V>?)?,
        prevKey: (K) -> K?,
        nextKey: (K) -> K?
    ): DataSource.Factory<K, V> = SeparatedPageGeneratorDataSource({ initial }, { pages[it] }, header, footer, prevKey, nextKey).cache()

    fun <K, V> separatedPages(
        initial: K,
        pages: Map<K, List<V>>,
        headers: Map<K, List<V>>?,
        footers: Map<K, List<V>>?,
        prevKey: (K) -> K?,
        nextKey: (K) -> K?
    ): DataSource.Factory<K, V> =
        if (pages.isEmpty() && headers.isNullOrEmpty() && footers.isNullOrEmpty()) {
            emptyContiguous()
        } else {
            SeparatedPageGeneratorDataSource(
                { initial },
                { pages[it] },
                { key, _ -> headers?.get(key) },
                { key, _ -> footers?.get(key) },
                prevKey,
                nextKey
            ).cache()
        }

    fun <K> pagesWithHeaders(
        initial: () -> K,
        page: (K) -> List<*>?,
        prevKey: (K) -> K?,
        nextKey: (K) -> K?
    ): DataSource.Factory<K, Any> = SeparatedPageGeneratorDataSource(initial, page, { key, _ -> listOf(key) }, null, prevKey, nextKey).cache()

    fun <K> pagesWithHeaders(
        initial: K,
        page: (K) -> List<*>?,
        prevKey: (K) -> K?,
        nextKey: (K) -> K?
    ): DataSource.Factory<K, Any> = SeparatedPageGeneratorDataSource({ initial }, page, { key, _ -> listOf(key) }, null, prevKey, nextKey).cache()

    fun <K> pagesWithHeaders(
        initial: K,
        pages: Map<K, List<*>>,
        prevKey: (K) -> K?,
        nextKey: (K) -> K?
    ): DataSource.Factory<K, Any> =
        if (pages.isEmpty()) emptyContiguous() else SeparatedPageGeneratorDataSource(
            { initial },
            { pages[it] },
            { key, _ -> listOf(key) },
            null,
            prevKey,
            nextKey
        ).cache()

    fun <K> pagesWithFooters(
        initial: () -> K,
        page: (K) -> List<*>?,
        prevKey: (K) -> K?,
        nextKey: (K) -> K?
    ): DataSource.Factory<K, Any> = SeparatedPageGeneratorDataSource(initial, page, null, { key, _ -> listOf(key) }, prevKey, nextKey).cache()

    fun <K> pagesWithFooters(
        initial: K,
        page: (K) -> List<*>?,
        prevKey: (K) -> K?,
        nextKey: (K) -> K?
    ): DataSource.Factory<K, Any> = SeparatedPageGeneratorDataSource({ initial }, page, null, { key, _ -> listOf(key) }, prevKey, nextKey).cache()

    fun <K> pagesWithFooters(
        initial: K,
        pages: Map<K, List<*>>,
        prevKey: (K) -> K?,
        nextKey: (K) -> K?
    ): DataSource.Factory<K, Any> =
        if (pages.isEmpty()) emptyContiguous() else SeparatedPageGeneratorDataSource(
            { initial },
            { pages[it] },
            null,
            { key, _ -> listOf(key) },
            prevKey,
            nextKey
        ).cache()
}

private class PageGeneratorDataSource<K, V>(
    private val initial: () -> K,
    private val page: (K) -> List<V>?,
    private val prevKey: (K) -> K?,
    private val nextKey: (K) -> K?
) : PageKeyedDataSource<K, V>() {

    override fun loadInitial(params: LoadInitialParams<K>, callback: LoadInitialCallback<K, V>) {
        val key = initial()
        val page = page(key) ?: emptyList()
        val prevKey = prevKey(key)
        val nextKey = nextKey(key)
        callback.onResult(page, prevKey, nextKey)
    }

    override fun loadAfter(params: LoadParams<K>, callback: LoadCallback<K, V>) {
        val key = params.key
        val page = page(key) ?: emptyList()
        val nextKey = nextKey(key)
        callback.onResult(page, nextKey)
    }

    override fun loadBefore(params: LoadParams<K>, callback: LoadCallback<K, V>) {
        val key = params.key
        val page = page(key) ?: emptyList()
        val prevKey = prevKey(key)
        callback.onResult(page, prevKey)
    }
}

private class SeparatedPageGeneratorDataSource<K, V>(
    private val initial: () -> K,
    private val page: (K) -> List<V>?,
    private val header: ((K, List<V>?) -> List<V>?)?,
    private val footer: ((K, List<V>?) -> List<V>?)?,
    private val prevKey: (K) -> K?,
    private val nextKey: (K) -> K?
) : PageKeyedDataSource<K, V>() {

    override fun loadInitial(params: LoadInitialParams<K>, callback: LoadInitialCallback<K, V>) {
        val key = initial()
        val page = page(key)
        val prevKey = prevKey(key)
        val nextKey = nextKey(key)
        callback.onResult(page, prevKey, nextKey)
    }

    override fun loadAfter(params: LoadParams<K>, callback: LoadCallback<K, V>) {
        val key = params.key
        val page = page(key)
        val nextKey = nextKey(key)
        callback.onResult(page, nextKey)
    }

    override fun loadBefore(params: LoadParams<K>, callback: LoadCallback<K, V>) {
        val key = params.key
        val page = page(key)
        val prevKey = prevKey(key)
        callback.onResult(page, prevKey)
    }

    private fun page(key: K): List<V> {
        val page = page.invoke(key)
        val header = header?.invoke(key, page)
        val footer = footer?.invoke(key, page)
        val hasPage = !page.isNullOrEmpty()
        val hasHeader = !header.isNullOrEmpty()
        val hasFooter = !footer.isNullOrEmpty()
        return if (hasPage && hasHeader && hasFooter) {
            header!! + page!! + footer!!
        } else if (hasPage && hasFooter) {
            page!! + footer!!
        } else if (hasPage && hasHeader) {
            header!! + page!!
        } else if (hasHeader && hasFooter) {
            header!! + footer!!
        } else if (hasHeader) {
            header!!
        } else if (hasFooter) {
            footer!!
        } else if (hasPage) {
            page!!
        } else {
            emptyList()
        }
    }
}

private class PreloadedPageKeyedDataSource<V>(private val array: Array<List<V>?>, private val keyOffset: Int, private val initialKey: Int?) : PageKeyedDataSource<Int, V>() {

    override fun loadInitial(params: LoadInitialParams<Int>, callback: LoadInitialCallback<Int, V>) {
        val key = initialKey ?: 0.toKey()
        val index = initialKey?.toIndex() ?: 0
        val prevKey = if (index == 0) null else key - 1
        val nextKey = if (index == array.size - 1) null else key + 1
        val page = page(index)
        callback.onResult(page, prevKey, nextKey)
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, V>) {
        val key = params.key
        val index = key.toIndex()
        val page = page(index)
        val nextKey = if (index == array.size - 1) null else key + 1
        callback.onResult(page, nextKey)
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, V>) {
        val key = params.key
        val index = key.toIndex()
        val page = page(index)
        val prevKey = if (index == 0) null else key - 1
        callback.onResult(page, prevKey)
    }

    private fun page(index: Int): List<V> = try {
        array[index]
    } catch (th: Throwable) {
        null
    } ?: emptyList()

    private fun Int.toIndex(): Int = this - keyOffset

    private fun Int.toKey(): Int = this + keyOffset
}

private class PreloadedDataSource<V>(private val array: Array<V>) : PositionalDataSource<V>() {

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<V>) {
        val index = params.requestedStartPosition
        val limit = params.requestedLoadSize
        callback.onResult(page(index, limit), index, array.size)
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<V>) {
        val index = params.startPosition
        val limit = params.loadSize
        callback.onResult(page(index, limit))
    }

    private fun page(from: Int, limit: Int): List<V> {
        val indexStart = max(from, 0)
        val indexEnd = min(indexStart + limit, array.size)
        return array.sliceArray(indexStart until indexEnd).asList()
    }
}

private object EmptyPageKeyedDataSource : PageKeyedDataSource<Nothing, Nothing>() {

    override fun loadInitial(params: LoadInitialParams<Nothing>, callback: LoadInitialCallback<Nothing, Nothing>) {
        callback.onResult(emptyList(), null, null)
    }

    override fun loadAfter(params: LoadParams<Nothing>, callback: LoadCallback<Nothing, Nothing>) {
        callback.onResult(emptyList(), null)
    }

    override fun loadBefore(params: LoadParams<Nothing>, callback: LoadCallback<Nothing, Nothing>) {
        callback.onResult(emptyList(), null)
    }

    object Factory : DataSource.Factory<Nothing, Nothing>() {
        override fun create(): DataSource<Nothing, Nothing> = EmptyPageKeyedDataSource
    }
}

private object EmptyPositionalDataSource : PositionalDataSource<Nothing>() {

    object Factory : DataSource.Factory<Int, Nothing>() {
        override fun create(): DataSource<Int, Nothing> = EmptyPositionalDataSource
    }

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Nothing>) {
        callback.onResult(emptyList(), 0, 0)
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Nothing>) {
        callback.onResult(emptyList())
    }
}

@Suppress("UNCHECKED_CAST")
private fun <K, V> DataSource<*, *>.cache(): DataSource.Factory<K, V> = CachedFactory(this) as DataSource.Factory<K, V>

private class CachedFactory<K, V>(private val dataSource: DataSource<K, V>) : DataSource.Factory<K, V>() {
    override fun create(): DataSource<K, V> = dataSource
}
