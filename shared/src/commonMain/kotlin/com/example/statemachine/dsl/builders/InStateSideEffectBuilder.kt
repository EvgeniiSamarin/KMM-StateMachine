package com.example.statemachine.dsl.builders

import com.example.statemachine.dsl.Action
import com.example.statemachine.dsl.GetState
import com.example.statemachine.dsl.SideEffect

internal abstract class InStateSideEffectBuilder<InputState : S, S, A> {

    internal abstract fun generateSideEffect(): SideEffect<S, Action<S, A>>

    internal suspend inline fun runOnlyIfInInputState(
        getState: GetState<S>,
        isInState: (S) -> Boolean,
        crossinline block: suspend (InputState) -> Unit,
    ) {
        val currentState = getState()
        if (isInState(currentState)) {
            val inputState = try {
                @Suppress("UNCHECKED_CAST")
                currentState as InputState
            } catch (e: ClassCastException) {
                return
            }

            block(inputState)
        }
    }
}
