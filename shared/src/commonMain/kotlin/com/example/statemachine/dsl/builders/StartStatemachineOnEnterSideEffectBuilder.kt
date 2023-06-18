package com.example.statemachine.dsl.builders

import com.example.statemachine.dsl.Action
import com.example.statemachine.dsl.ChangeStateAction
import com.example.statemachine.dsl.ExternalWrappedAction
import com.example.statemachine.dsl.GetState
import com.example.statemachine.dsl.SideEffect
import com.example.statemachine.dsl.State
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
internal class StartStatemachineOnEnterSideEffectBuilder<SubStateMachineState : Any, SubStateMachineAction : Any, InputState : S, S : Any, A>(
    private val subStateMachineFactory: (InputState) -> StateMachine<SubStateMachineState, SubStateMachineAction>,
    private val actionMapper: (A) -> SubStateMachineAction?,
    private val stateMapper: (State<InputState>, SubStateMachineState) -> ChangedState<S>,
    private val isInState: (S) -> Boolean,
) : InStateSideEffectBuilder<InputState, S, A>() {

    override fun generateSideEffect(): SideEffect<S, Action<S, A>> {
        return { actions: Flow<Action<S, A>>, getState: GetState<S> ->
            dispatchActionsToSubStateMachineAndCollectSubStateMachineState(actions, getState)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun dispatchActionsToSubStateMachineAndCollectSubStateMachineState(
        upstreamActions: Flow<Action<S, A>>,
        getState: GetState<S>,
    ): Flow<Action<S, A>> {
        return upstreamActions
            .whileInState(isInState, getState) { actions: Flow<Action<S, A>> ->
                val stateOnEntering = getState() as? InputState
                if (stateOnEntering == null) {
                    emptyFlow()
                } else {
                    val coroutineWaiter = CoroutineWaiter()

                    var subStateMachine: StateMachine<SubStateMachineState, SubStateMachineAction>? =
                        subStateMachineFactory(stateOnEntering)

                    actions
                        .onEach { action ->
                            if (action is ExternalWrappedAction<S, A>) {
                                coroutineScope {
                                    launch {
                                        coroutineWaiter.waitUntilResumed()
                                        actionMapper(action.action)?.let {
                                            subStateMachine?.dispatch(it)
                                        }
                                    }
                                }
                            }
                        }
                        .mapToIsInState(isInState, getState)
                        .flatMapLatest { inState: Boolean ->
                            if (inState) {
                                val currentState = getState() as? InputState
                                if (currentState == null) {
                                    emptyFlow()
                                } else {
                                    subStateMachine?.state?.onStart {
                                        coroutineWaiter.resume()
                                    } ?: emptyFlow()
                                }
                            } else {
                                emptyFlow()
                            }.mapNotNull { subStateMachineState: SubStateMachineState ->
                                var changeStateAction: ChangeStateAction<S, A>? = null

                                runOnlyIfInInputState(getState, isInState) { inputState ->
                                    changeStateAction = ChangeStateAction<S, A>(
                                        runReduceOnlyIf = isInState,
                                        changedState = stateMapper(
                                            State(inputState),
                                            subStateMachineState,
                                        ),
                                    )
                                }

                                changeStateAction
                            }
                        }
                        .onCompletion {
                            subStateMachine = null // очищаем чтобы не было утечек
                        }
                }
            }
    }
}
