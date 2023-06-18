package example

import com.example.statemachine.dsl.AbstractStateMachine
import com.example.statemachine.dsl.ChangedState
import com.example.statemachine.dsl.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InternalPaginationStateMachine(
    private val todoApi: TodoApi,
) : AbstractStateMachine<PaginationState, Action>(LoadFirstPagePaginationState) {
    init {
        spec {
            inState<LoadFirstPagePaginationState> {
                onEnter { loadFirstPage(it) }
            }
            inState<LoadingFirstPageError> {
                on<RetryLoadingFirstPage> { _, state ->
                    state.override { LoadFirstPagePaginationState }
                }
            }
            inState<ShowContentPaginationState> {
                on<LoadNextPage> { _, state ->
                    moveToLoadNextPageStateIfCanLoadNextPage(state)
                }
            }
            inState<ShowContentPaginationState>(additionalIsInState = {
                it.canLoadNextPage && it.nextPageLoadingState == NextPageLoadingState.LOADING
            }) {
                onEnter { loadNextPage(it) }
            }
            inState<ShowContentPaginationState>(additionalIsInState = {
                it.nextPageLoadingState == NextPageLoadingState.ERROR
            }) {
                onEnter { showPaginationErrorFor3SecsThenReset(it) }
            }
            inState<ShowContentPaginationState> {
                onActionStartStateMachine(
                    stateMachineFactory = { action: ToggleFavoriteAction, state: ShowContentPaginationState ->
                        val repo = state.items.find { it.id == action.id }!!
                        MarkAsFavoriteStateMachine(
                            todoApi = todoApi,
                            repository = repo,
                        )
                    },
                ) { inputState: State<ShowContentPaginationState>, childState: TodoRepository ->
                    inputState.mutate {
                        copy(
                            items = items.map { repoItem ->
                                if (repoItem.id == childState.id) {
                                    childState
                                } else {
                                    repoItem
                                }
                            },
                        )
                    }
                }
            }
        }
    }
    private fun moveToLoadNextPageStateIfCanLoadNextPage(
        state: State<ShowContentPaginationState>,
    ): ChangedState<PaginationState> {
        return if (!state.snapshot.canLoadNextPage) {
            state.noChange()
        } else {
            state.mutate {
                copy(
                    nextPageLoadingState = NextPageLoadingState.LOADING,
                )
            }
        }
    }
    private suspend fun loadFirstPage(
        state: State<LoadFirstPagePaginationState>,
    ): ChangedState<PaginationState> {
        val nextState = try {
            when (val pageResult: PageResult = todoApi.loadPage(page = 0)) {
                PageResult.NoNextPage -> {
                    ShowContentPaginationState(
                        items = emptyList(),
                        canLoadNextPage = false,
                        currentPage = 1,
                        nextPageLoadingState = NextPageLoadingState.IDLE,
                    )
                }
                is PageResult.Page -> {
                    ShowContentPaginationState(
                        items = pageResult.items,
                        canLoadNextPage = true,
                        currentPage = pageResult.page,
                        nextPageLoadingState = NextPageLoadingState.IDLE,
                    )
                }
            }
        } catch (t: Throwable) {
            LoadingFirstPageError(t)
        }
        return state.override { nextState }
    }

    private suspend fun loadNextPage(
        state: State<ShowContentPaginationState>,
    ): ChangedState<PaginationState> {
        val nextPageNumber = state.snapshot.currentPage + 1
        val nextState: ChangedState<ShowContentPaginationState> = try {
            when (val pageResult = todoApi.loadPage(page = nextPageNumber)) {
                PageResult.NoNextPage -> {
                    state.mutate {
                        copy(
                            nextPageLoadingState = NextPageLoadingState.IDLE,
                            canLoadNextPage = false,
                        )
                    }
                }
                is PageResult.Page -> {
                    state.mutate {
                        copy(
                            items = items + pageResult.items,
                            canLoadNextPage = true,
                            currentPage = nextPageNumber,
                            nextPageLoadingState = NextPageLoadingState.IDLE,
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            state.mutate {
                copy(
                    nextPageLoadingState = NextPageLoadingState.ERROR,
                )
            }
        }
        return nextState
    }
    private suspend fun showPaginationErrorFor3SecsThenReset(
        state: State<ShowContentPaginationState>,
    ): ChangedState<PaginationState> {
        delay(3000)
        return state.mutate {
            copy(
                nextPageLoadingState = NextPageLoadingState.IDLE,
            )
        }
    }
}

/**
 * A wrapper class around [InternalPaginationStateMachine] so that you dont need to deal with `Flow`
 * and suspend functions from iOS.
 */
class PaginationStateMachine(
    todoApi: TodoApi,
    private val scope: CoroutineScope,
) {
    private val stateMachine = InternalPaginationStateMachine(todoApi = todoApi)

    fun dispatch(action: Action) {
        scope.launch {
            stateMachine.dispatch(action)
        }
    }

    fun start(stateChangeListener: (PaginationState) -> Unit) {
        scope.launch {
            stateMachine.state.collect {
                stateChangeListener(it)
            }
        }
    }
}
