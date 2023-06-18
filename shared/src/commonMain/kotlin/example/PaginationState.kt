package example

sealed class PaginationState

object LoadFirstPagePaginationState : PaginationState()

data class LoadingFirstPageError(val cause: Throwable) : PaginationState()

enum class NextPageLoadingState {
    IDLE,
    LOADING,
    ERROR,
}

data class ShowContentPaginationState(
    val items: List<TodoRepository>,
    val nextPageLoadingState: NextPageLoadingState,
    internal val currentPage: Int,
    internal val canLoadNextPage: Boolean,
) : PaginationState()
