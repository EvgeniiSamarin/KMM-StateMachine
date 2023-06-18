package com.example.statemachine.dsl.builders

import com.example.statemachine.dsl.Action
import com.example.statemachine.dsl.ChangeStateAction
import com.example.statemachine.dsl.ChangedState
import com.example.statemachine.dsl.GetState
import com.example.statemachine.dsl.SideEffect
import com.example.statemachine.dsl.State
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

@ExperimentalCoroutinesApi
internal class OnEnterInStateSideEffectBuilder<InputState : S, S : Any, A : Any>(
    private val isInState: (S) -> Boolean,
    private val handler: suspend (state: State<InputState>) -> ChangedState<S>,
) : InStateSideEffectBuilder<InputState, S, A>() {

    override fun generateSideEffect(): SideEffect<S, Action<S, A>> {
        return { actions: Flow<Action<S, A>>, getState: GetState<S> ->
            actions
                .mapToIsInState(isInState, getState)
                .flatMapLatest {
                    if (it) {
                        setStateFlow(getState)
                    } else {
                        emptyFlow()
                    }
                }
        }
    }

    private suspend fun setStateFlow(
        getState: GetState<S>,
    ): Flow<Action<S, A>> = flow {
        runOnlyIfInInputState(getState, isInState) { inputState ->
            val changeState = handler(State(inputState))
            emit(
                ChangeStateAction<S, A>(
                    changedState = changeState,
                    runReduceOnlyIf = { state -> isInState(state) },
                ),
            )
        }
    }
}
