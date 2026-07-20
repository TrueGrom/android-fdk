package grmv.android.fdk.repository.paging

/**
 * A single page of results plus the pagination cursors needed by Paging 3.
 *
 * @param items Items on this page, in display order.
 * @param nextPage 1-based key for the next page, or `null` when this is the last page.
 * @param prevPage 1-based key for the previous page, or `null` when this is the first page.
 * [after]/[before] are only consumed when [createPager] is called with `enablePlaceholders = true`
 * (the default); with placeholders disabled they are ignored and may be `0`.
 *
 * @param after Number of items that exist after this page; used by Paging 3 to size the
 *   trailing placeholder region. Pass `0` on the last page (or when placeholders are disabled).
 * @param before Number of items that exist before this page; used by Paging 3 to size the
 *   leading placeholder region. Pass `0` on the first page (or when placeholders are disabled).
 */
data class PagingResult<T : Any>(
    val items: List<T>,
    val nextPage: Int?,
    val prevPage: Int?,
    val after: Int,
    val before: Int
)
