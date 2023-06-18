@file:Suppress("UNCHECKED_CAST")

package com.example.statemachine.dsl.builders

import com.example.statemachine.dsl.Action
import com.example.statemachine.dsl.ChangeStateAction
import com.example.statemachine.dsl.ChangedState
import com.example.statemachine.dsl.ExternalWrappedAction
import com.example.statemachine.dsl.GetState
import com.example.statemachine.dsl.InitialStateAction
import com.example.statemachine.dsl.SideEffect
import com.example.statemachine.dsl.State
import kotlin.reflect.KClass
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class StartStateMachineOnActionInStateSideEffectBuilder<SubStateMachineState : Any, SubStateMachineAction : Any, InputState : S, ActionThatTriggeredStartingStateMachine : A, S : Any, A : Any>(
    private val subStateMachineFactory: (
        action: ActionThatTriggeredStartingStateMachine,
        state: InputState,
    ) -> StateMachine<SubStateMachineState, SubStateMachineAction>,
    private val actionMapper: (A) -> SubStateMachineAction?,
    private val stateMapper: (State<InputState>, SubStateMachineState) -> ChangedState<S>,
    private val isInState: (S) -> Boolean,
    internal val subActionClass: KClass<out A>,
) : InStateSideEffectBuilder<InputState, S, A>() {

    override fun generateSideEffect(): SideEffect<S, Action<S, A>> {
        return { actions: Flow<Action<S, A>>, getState: GetState<S> ->

            actions.whileInState(isInState, getState) { inStateAction ->
                channelFlow {
                    val subStateMachinesMap = SubStateMachinesMap<SubStateMachineState, SubStateMachineAction, ActionThatTriggeredStartingStateMachine>()

                    inStateAction
                        .collect { action ->
                            when (action) {
                                is ChangeStateAction,
                                is InitialStateAction,
                                -> {

                                is ExternalWrappedAction<*, *> ->
                                    runOnlyIfInInputState(getState, isInState) { currentState ->
                                        // TODO take ExecutionPolicy into account
                                        if (subActionClass.isInstance(action.action)) {
                                            val actionThatStartsStateMachine =
                                                action.action as ActionThatTriggeredStartingStateMachine

                                            val stateMachine = subStateMachineFactory(
                                                actionThatStartsStateMachine,
                                                currentState,
                                            )
                                            val coroutineWaiter = CoroutineWaiter()
                                            val job = launch {
                                                stateMachine.state
                                                    .onStart {
                                                        coroutineWaiter.resume()
                                                    }
                                                    .onCompletion {
                                                        subStateMachinesMap.remove(stateMachine)
                                                    }
                                                    .collect { subStateMachineState ->
                                                        runOnlyIfInInputState(getState, isInState) { parentState ->
                                                            send(
                                                                ChangeStateAction(
                                                                    runReduceOnlyIf = isInState,
                                                                    changedState = stateMapper(
                                                                        State(parentState),
                                                                        subStateMachineState,
                                                                    ),
                                                                ),
                                                            )
                                                        }
                                                    }
                                            }
                                            subStateMachinesMap.cancelPreviousAndAddNew(
                                                actionThatStartedStateMachine = actionThatStartsStateMachine,
                                                stateMachine = stateMachine,
                                                job = job,
                                                coroutineWaiter = coroutineWaiter,
                                            )
                                        } else {
                                            subStateMachinesMap.forEachStateMachine { stateMachine, coroutineWaiter ->
                                                launch {
                                                    coroutineWaiter.waitUntilResumed()
                                                    actionMapper(action.action as A)?.let {
                                                        stateMachine.dispatch(it)
                                                    }
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                }
            }
        }
    }

    internal class SubStateMachinesMap<S : Any, A : Any, ActionThatTriggeredStartingStateMachine : Any> {
        internal data class StateMachineAndJob<S : Any, A : Any>(
            internal val stateMachine: StateMachine<S, A>,
            internal val job: Job,
            internal val coroutineWaiter: CoroutineWaiter,
        )

        private val mutex = Mutex()
        private val stateMachinesAndJobsMap = LinkedHashMap<ActionThatTriggeredStartingStateMachine, StateMachineAndJob<S, A>>()

        suspend fun size(): Int = mutex.withLock { stateMachinesAndJobsMap.size }

        suspend fun cancelPreviousAndAddNew(
            actionThatStartedStateMachine: ActionThatTriggeredStartingStateMachine,
            stateMachine: StateMachine<S, A>,
            coroutineWaiter: CoroutineWaiter,
            job: Job,
        ) {
            mutex.withLock {
                val existingStateMachinesAndJobs: StateMachineAndJob<S, A>? = stateMachinesAndJobsMap[actionThatStartedStateMachine]
                existingStateMachinesAndJobs?.job?.cancel()

                stateMachinesAndJobsMap[actionThatStartedStateMachine] = StateMachineAndJob(
                    stateMachine = stateMachine,
                    job = job,
                    coroutineWaiter = coroutineWaiter,
                )
            }
        }

        suspend inline fun forEachStateMachine(
            crossinline block: suspend (StateMachine<S, A>, CoroutineWaiter) -> Unit,
        ) {
            mutex.withLock {
                stateMachinesAndJobsMap.values.forEach { stateMachineAndJob ->
                    block(stateMachineAndJob.stateMachine, stateMachineAndJob.coroutineWaiter)
                }
            }
        }

        suspend fun remove(stateMachine: StateMachine<S, A>): StateMachineAndJob<S, A>? {
            // could be optimized for better runtime
            val result = mutex.withLock {
                var key: ActionThatTriggeredStartingStateMachine? = null
                for ((actionThatTriggeredStarting, stateMachineAndJob) in stateMachinesAndJobsMap) {
                    if (stateMachineAndJob.stateMachine === stateMachine) {
                        key = actionThatTriggeredStarting
                        break
                    }
                }

                if (key != null) {
                    stateMachinesAndJobsMap.remove(key)
                } else {
                    null
                }
            }

            return result
        }
    }
}
