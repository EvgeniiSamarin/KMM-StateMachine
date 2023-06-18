package com.example.statemachine.dsl

internal data class OverrideState<S>(internal val newState: S) : ChangedState<S>()
internal object NoStateChange : ChangedState<Nothing>()

/**
 * Изменение состояния
 */
public fun <S> ChangedState<S>.reduce(state: S): S {
    return when (this) {
        is NoStateChange -> state
        is OverrideState -> newState
        is UnsafeMutateState<*, S> -> this.reduceImpl(state)
    }
}
