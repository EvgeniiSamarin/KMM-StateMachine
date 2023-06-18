package example

import com.example.statemachine.dsl.AbstractStateMachine
import com.example.statemachine.dsl.State
import kotlinx.coroutines.delay

class MarkAsFavoriteStateMachine(
    private val todoApi: TodoApi,
    repository: TodoRepository,
) : AbstractStateMachine<TodoRepository, Action>(
    initialState = repository.copy(favoriteStatus = FavoriteStatus.OPERATION_IN_PROGRESS),
) {
    private val favoriteStatusWhenStarting: FavoriteStatus = repository.favoriteStatus

    init {
        spec {
            inState<TodoRepository>(additionalIsInState = {
                it.favoriteStatus == FavoriteStatus.OPERATION_IN_PROGRESS
            }) {
                onEnter { markAsFavorite(it) }
            }

            inState<TodoRepository>(additionalIsInState = {
                it.favoriteStatus == FavoriteStatus.OPERATION_FAILED
            }) {
                onEnter { resetErrorStateAfter3Seconds(it) }
                on<RetryToggleFavoriteAction> { action, state -> resetErrorState(action, state) }
            }
        }
    }

    private suspend fun markAsFavorite(state: State<TodoRepository>): ChangedState<TodoRepository> {
        return try {
            val shouldBeMarkedAsFavorite = favoriteStatusWhenStarting == FavoriteStatus.NOT_FAVORITE
            todoApi.markAsFavorite(
                repoId = state.snapshot.id,
                favorite = shouldBeMarkedAsFavorite,
            )
            state.mutate {
                copy(
                    favoriteStatus = if (shouldBeMarkedAsFavorite) {
                        FavoriteStatus.FAVORITE
                    } else {
                        FavoriteStatus.NOT_FAVORITE
                    },
                    stargazersCount = if (shouldBeMarkedAsFavorite) {
                        stargazersCount + 1
                    } else {
                        stargazersCount - 1
                    },
                )
            }
        } catch (e: Exception) {
            state.mutate { copy(favoriteStatus = FavoriteStatus.OPERATION_FAILED) }
        }
    }

    private suspend fun resetErrorStateAfter3Seconds(state: State<TodoRepository>): ChangedState<TodoRepository> {
        delay(3000)
        return state.mutate { copy(favoriteStatus = favoriteStatusWhenStarting) }
    }

    private fun resetErrorState(action: RetryToggleFavoriteAction, state: State<TodoRepository>): ChangedState<TodoRepository> {
        return if (action.id != state.snapshot.id) {
            // Since all active MarkAsFavoriteStateMachine receive this action
            // we need to ignore those who are not meant for this state machine
            state.noChange()
        } else {
            state.mutate { copy(favoriteStatus = FavoriteStatus.OPERATION_IN_PROGRESS) }
        }
    }
}
