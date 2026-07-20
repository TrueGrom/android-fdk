package grmv.android.fdk.repository.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import grmv.android.fdk.coroutines.rethrowCancellation

/**
 * Builds a Paging 3 [Pager] backed by a 1-based page-number data source.
 *
 * @param pageSize Number of items per page; defaults to 25.
 * @param initialLoadSize Number of items to load on the first refresh; defaults to `pageSize * 2`.
 * @param prefetchDistance How far from the loaded edge (in items) a scroll triggers the next
 *   page load; defaults to 5.
 * @param enablePlaceholders Whether Paging 3 renders placeholders for not-yet-loaded items.
 *   When `true` (default), [PagingResult.after]/[PagingResult.before] must be accurate item
 *   counts; when `false`, they are ignored and may be `0`.
 * @param dataSource Suspend function that fetches a single page. Receives the 1-based page
 *   number (first load uses key `1`) and must return a [PagingResult] for that page.
 * @return A configured [Pager] whose [androidx.paging.PagingData] stream can be collected
 *   via [Pager.flow].
 */
fun <T : Any> createPager(
    pageSize: Int = 25,
    initialLoadSize: Int = pageSize * 2,
    prefetchDistance: Int = 5,
    enablePlaceholders: Boolean = true,
    dataSource: suspend (Int) -> PagingResult<T>,
): Pager<Int, T> {
    return Pager(
        config = PagingConfig(
            pageSize = pageSize,
            initialLoadSize = initialLoadSize,
            prefetchDistance = prefetchDistance,
            enablePlaceholders = enablePlaceholders,
        ),
        pagingSourceFactory = { DefaultPagingSource(dataSource) }
    )
}

internal class DefaultPagingSource<T : Any>(
    private val dataSource: suspend (Int) -> PagingResult<T>,
) : PagingSource<Int, T>() {

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val page = params.key ?: 1
        return try {
            val result = dataSource.invoke(page)
            LoadResult.Page(
                data = result.items,
                prevKey = result.prevPage,
                nextKey = result.nextPage,
                itemsAfter = result.after,
                itemsBefore = result.before
            )
        } catch (e: Throwable) {
            e.rethrowCancellation()
            LoadResult.Error(e)
        }
    }
}
