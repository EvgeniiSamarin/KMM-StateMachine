package com.example.statemachine.dsl.builders

import com.example.statemachine.dsl.Action
import com.example.statemachine.dsl.ChangeStateAction
import com.example.statemachine.dsl.ChangedState
import com.example.statemachine.dsl.ExecutionPolicy
import com.example.statemachine.dsl.GetState
import com.example.statemachine.dsl.SideEffect
import com.example.statemachine.dsl.State
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull


@ExperimentalCoroutinesApi
internal class CollectInStateBasedOnStateBuilder<T, InputState : S, S : Any, A : Any>(
    private val isInState: (S) -> Boolean,
    private val flowBuilder: (Flow<InputState>) -> Flow<T>,
    private val executionPolicy: ExecutionPolicy,
    private val handler: suspend (item: T, state: State<InputState>) -> ChangedState<S>,
) : InStateSideEffectBuilder<InputState, S, A>() {

    override fun generateSideEffect(): SideEffect<S, Action<S, A>> {
        return { actions: Flow<Action<S, A>>, getState: GetState<S> ->
            actions.whileInState(isInState, getState) { inStateActions ->
                flowOfCurrentState(inStateActions, getState)
                    .transformWithFlowBuilder()
                    .flatMapWithExecutionPolicy(executionPolicy) {
                        setStateFlow(value = it, getState = getState)
                    }
            }
        }
    }

    @Suppress("unchecked_cast")
    private fun flowOfCurrentState(
        actions: Flow<Action<S, A>>,
        getState: GetState<S>,
    ): Flow<InputState> {
        return actions.mapNotNull { getState() as? InputState }
            .distinctUntilChanged()
    }

    private fun Flow<InputState>.transformWithFlowBuilder(): Flow<T> {
        return flowBuilder(this)
    }

    private suspend fun setStateFlow(
        value: T,
        getState: GetState<S>,
    ): Flow<Action<S, A>> = flow {
        runOnlyIfInInputState(getState, isInState) { inputState ->
            val changeState = handler(value, State(inputState))
            emit(
                ChangeStateAction<S, A>(
                    changedState = changeState,
                    runReduceOnlyIf = { state -> isInState(state) },
                ),
            )
        }
    }
}
