package com.example.statemachine.dsl

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

internal fun <A, S> Flow<A>.reduxStore(
    initialStateSupplier: () -> S,
    sideEffects: Iterable<SideEffect<S, A>>,
    reducer: Reducer<S, A>,
): Flow<S> = flow {
    var currentState: S = initialStateSupplier()
    val getState: GetState<S> = { currentState }

    // Emit the initial state
    emit(currentState)

    val loopbacks = sideEffects.map {
        Channel<A>()
    }
    val sideEffectActions = sideEffects.mapIndexed { index, sideEffect ->
        val actionsFlow = loopbacks[index].consumeAsFlow()
        sideEffect(actionsFlow, getState)
    }
    val upstreamActions = this@reduxStore

    (sideEffectActions + upstreamActions).merge().collect { action ->
        // Change state
        val newState: S = reducer(currentState, action)
        currentState = newState
        emit(newState)

        // broadcast action
        loopbacks.forEach {
            it.send(action)
        }
    }
}
