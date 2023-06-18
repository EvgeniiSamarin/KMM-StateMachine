package example

sealed interface Action

object RetryLoadingFirstPage : Action
object LoadNextPage : Action
data class ToggleFavoriteAction(val id: String) : Action
data class RetryToggleFavoriteAction(val id: String) : Action
