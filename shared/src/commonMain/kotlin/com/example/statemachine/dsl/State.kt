package com.example.statemachine.dsl


public class State<InputState : Any>(
    public val snapshot: InputState,
) {
    public fun mutate(reducer: InputState.() -> InputState): ChangedState<InputState> {
        return UnsafeMutateState(reducer)
    }

    public fun <S : Any> override(reducer: InputState.() -> S): ChangedState<S> {
        return UnsafeMutateState(reducer)
    }

    public fun <S : Any> noChange(): ChangedState<S> {
        return NoStateChange
    }
}

public sealed class ChangedState<out S>

internal class UnsafeMutateState<InputState, S>(
    internal val reducer: InputState.() -> S,
) : ChangedState<S>() {
    @Suppress("UNCHECKED_CAST")
    internal fun reduceImpl(state: S): S =
        reducer(state as InputState)
}
