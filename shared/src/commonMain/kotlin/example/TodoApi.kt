package example

import kotlinx.coroutines.delay

class TodoApi {

    internal var githubData = (0..120).map {
        TodoRepository(
            id = "$it",
            name = "TODO $it",
            stargazersCount = 0,
            favoriteStatus = FavoriteStatus.NOT_FAVORITE,
        )
    }

    private val pageSize = 30

    // Used to simulate network errors
    private var counter = 0
    private fun shouldFail(): Boolean = counter++ % 4 == 0

    suspend fun loadPage(page: Int): PageResult {
        delay(2000)
        if (shouldFail()) {
            throw Exception("Faked network error")
        }
        val start = page * pageSize
        val end = min(githubData.size, page * pageSize + pageSize)

        return (
            if (start < githubData.size) {
                githubData.subList(
                    start,
                    end,
                )
            } else {
                emptyList<TodoRepository>()
            }
            ).run {
            if (isEmpty()) {
                PageResult.NoNextPage
            } else {
                PageResult.Page(page = page, items = this)
            }
        }
    }

    @Suppress("unused_parameter")
    suspend fun markAsFavorite(repoId: String, favorite: Boolean) {
        delay(2000) // simulate network effect
        if (shouldFail()) {
            throw Exception("Faked network error")
        }
    }

    private fun min(a: Int, b: Int): Int = if (a < b) a else b
}

sealed class PageResult {
    internal object NoNextPage : PageResult()
    internal data class Page(val page: Int, val items: List<TodoRepository>) : PageResult()
}

fun List<TodoRepository>.markAsFavorite(repoId: String, favorite: Boolean): List<TodoRepository> {
    return map {
        if (it.id == repoId) {
            it.copy(
                favoriteStatus = if (favorite) {
                    FavoriteStatus.FAVORITE
                } else {
                    FavoriteStatus.NOT_FAVORITE
                },
            )
        } else {
            it
        }
    }
}
