package com.example.statemachine.android

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch

@Composable
fun PopularTodoUi(state: PaginationState?, dispatch: (Action) -> Unit) {
    val scaffoldState = rememberScaffoldState()
    SampleTheme {
        Scaffold(scaffoldState = scaffoldState) {
            when (state) {
                null, // null means state machine did not emit yet the first state --> in mean time show Loading
                is LoadFirstPagePaginationState,
                -> LoadingUi(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                )
                is LoadingFirstPageError -> ErrorUi(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it),
                    dispatch = dispatch,
                )
                is ShowContentPaginationState -> {
                    val showLoadNextPageUi = state.shouldShowLoadMoreIndicator()
                    val showErrorSnackBar = state.shouldShowErrorSnackbar()

                    TodosListUI(
                        repos = state.items,
                        loadMore = showLoadNextPageUi,
                        dispatch = dispatch,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it),
                    )

                    val errorMessage = stringResource(R.string.unexpected_error)
                    if (showErrorSnackBar) {
                        LaunchedEffect(scaffoldState.snackbarHostState) {
                            launch {
                                scaffoldState.snackbarHostState.showSnackbar(
                                    errorMessage,
                                    duration = SnackbarDuration.Indefinite, // Will be dismissed by changing state
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ShowContentPaginationState.shouldShowLoadMoreIndicator(): Boolean = when (this.nextPageLoadingState) {
    NextPageLoadingState.LOADING -> true
    else -> false
}

private fun ShowContentPaginationState.shouldShowErrorSnackbar(): Boolean = when (this.nextPageLoadingState) {
    NextPageLoadingState.ERROR -> true
    else -> false
}
