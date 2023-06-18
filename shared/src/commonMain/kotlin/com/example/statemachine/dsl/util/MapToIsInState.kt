package com.example.statemachine.dsl.util

import com.example.statemachine.dsl.Action
import com.example.statemachine.dsl.GetState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal fun <S, A> Flow<Action<S, A>>.mapToIsInState(
    isInState: (S) -> Boolean,
    getState: GetState<S>,
): Flow<Boolean> {
    return map { isInState(getState()) }
        .distinctUntilChanged()
}
