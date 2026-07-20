package grmv.android.fdk.repository.paging

import androidx.paging.PagingSource
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PagerTest {

    private fun pageOf(value: Int) = PagingResult(
        items = listOf(value),
        nextPage = value + 1,
        prevPage = value - 1,
        after = 5,
        before = 3,
    )

    private fun refreshParams(key: Int?, placeholdersEnabled: Boolean = true) =
        PagingSource.LoadParams.Refresh(key, loadSize = 10, placeholdersEnabled = placeholdersEnabled)

    // --- DefaultPagingSource.load ---

    @Test
    fun `load - requested key - forwards key to data source and maps page`() = runTest {
        var requested = -1
        val source = DefaultPagingSource<Int> { page ->
            requested = page
            pageOf(page)
        }

        val result = source.load(refreshParams(3))

        assertEquals(3, requested)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(listOf(3), page.data)
        assertEquals(4, page.nextKey)
        assertEquals(2, page.prevKey)
        assertEquals(5, page.itemsAfter)
        assertEquals(3, page.itemsBefore)
    }

    @Test
    fun `load - placeholders disabled - still maps page and carries counts`() = runTest {
        val source = DefaultPagingSource<Int> { page -> pageOf(page) }

        val result = source.load(refreshParams(3, placeholdersEnabled = false))

        val page = result as PagingSource.LoadResult.Page
        assertEquals(listOf(3), page.data)
        assertEquals(4, page.nextKey)
        assertEquals(2, page.prevKey)
        assertEquals(5, page.itemsAfter)
        assertEquals(3, page.itemsBefore)
    }

    @Test
    fun `load - null key - defaults to page one`() = runTest {
        var requested = -1
        val source = DefaultPagingSource<Int> { page ->
            requested = page
            pageOf(page)
        }

        source.load(refreshParams(null))

        assertEquals(1, requested)
    }

    @Test
    fun `load - data source throws - returns LoadResult Error`() = runTest {
        val boom = IllegalStateException("boom")
        val source = DefaultPagingSource<Int> { throw boom }

        val result = source.load(refreshParams(1))

        val error = result as PagingSource.LoadResult.Error
        assertSame(boom, error.throwable)
    }

    @Test
    fun `load - data source cancelled - rethrows CancellationException`() = runTest {
        val source = DefaultPagingSource<Int> { throw CancellationException("cancelled") }

        var thrown = false
        try {
            source.load(refreshParams(1))
        } catch (e: CancellationException) {
            thrown = true
        }

        assertTrue(thrown)
    }
}
