package example

data class TodoRepository(
    val id: String,
    val name: String,
    val stargazersCount: Int,
    val favoriteStatus: FavoriteStatus,
)
