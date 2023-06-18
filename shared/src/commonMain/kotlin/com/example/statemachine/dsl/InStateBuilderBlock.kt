package com.example.statemachine.dsl

import kotlin.reflect.KClass
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@FlowReduxDsl
public class InStateBuilderBlock<InputState : S, S : Any, A : Any>(
    private val _isInState: (S) -> Boolean,
) {

    private val _inStateSideEffectBuilders = ArrayList<InStateSideEffectBuilder<InputState, S, A>>()

    internal fun generateSideEffects(): List<SideEffect<S, Action<S, A>>> {
        return _inStateSideEffectBuilders.map { it.generateSideEffect() }
    }

    public inline fun <reified SubAction : A> on(
        executionPolicy: ExecutionPolicy = ExecutionPolicy.CANCEL_PREVIOUS,
        noinline handler: suspend (action: SubAction, state: State<InputState>) -> ChangedState<S>,
    ) {
        on(SubAction::class, executionPolicy, handler)
    }

    @PublishedApi
    internal fun <SubAction : A> on(
        actionClass: KClass<SubAction>,
        executionPolicy: ExecutionPolicy,
        handler: suspend (action: SubAction, state: State<InputState>) -> ChangedState<S>,
    ) {
        val builder = OnActionInStateSideEffectBuilder<InputState, S, A>(
            executionPolicy = executionPolicy,
            subActionClass = actionClass,
            isInState = _isInState,
            handler = { action, state ->
                @Suppress("UNCHECKED_CAST")
                handler(action as SubAction, state)
            },
        )

        _inStateSideEffectBuilders.add(builder)
    }

    public inline fun <reified SubAction : A> onActionEffect(
        executionPolicy: ExecutionPolicy = ExecutionPolicy.CANCEL_PREVIOUS,
        noinline handler: suspend (action: SubAction, stateSnapshot: InputState) -> Unit,
    ) {
        onActionEffect(SubAction::class, executionPolicy, handler)
    }

    @PublishedApi
    internal fun <SubAction : A> onActionEffect(
        actionClass: KClass<SubAction>,
        executionPolicy: ExecutionPolicy,
        handler: suspend (action: SubAction, stateSnapshot: InputState) -> Unit,
    ) {
        on(
            actionClass = actionClass,
            executionPolicy = executionPolicy,
            handler = { action: SubAction, state: State<InputState> ->
                handler(action, state.snapshot)
                NoStateChange
            },
        )
    }

    public fun onEnter(
        handler: suspend (state: State<InputState>) -> ChangedState<S>,
    ) {
        _inStateSideEffectBuilders.add(
            OnEnterInStateSideEffectBuilder(
                isInState = _isInState,
                handler = handler,
            ),
        )
    }

    public fun onEnterEffect(
        handler: suspend (stateSnapshot: InputState) -> Unit,
    ) {
        onEnter { state ->
            handler(state.snapshot)
            NoStateChange
        }
    }

    public fun <T> collectWhileInState(
        flow: Flow<T>,
        executionPolicy: ExecutionPolicy = ExecutionPolicy.ORDERED,
        handler: suspend (item: T, state: State<InputState>) -> ChangedState<S>,
    ) {
        _inStateSideEffectBuilders.add(
            CollectInStateBuilder(
                isInState = _isInState,
                flow = flow,
                executionPolicy = executionPolicy,
                handler = handler,
            ),
        )
    }
    public fun <T> collectWhileInState(
        flowBuilder: (state: Flow<InputState>) -> Flow<T>,
        executionPolicy: ExecutionPolicy = ExecutionPolicy.ORDERED,
        handler: suspend (item: T, state: State<InputState>) -> ChangedState<S>,
    ) {
        _inStateSideEffectBuilders.add(
            CollectInStateBasedOnStateBuilder(
                isInState = _isInState,
                flowBuilder = flowBuilder,
                executionPolicy = executionPolicy,
                handler = handler,
            ),
        )
    }

    public fun <T> collectWhileInStateEffect(
        flow: Flow<T>,
        executionPolicy: ExecutionPolicy = ExecutionPolicy.ORDERED,
        handler: suspend (item: T, state: InputState) -> Unit,
    ) {
        collectWhileInState(
            flow = flow,
            executionPolicy = executionPolicy,
            handler = { value: T, state: State<InputState> ->
                handler(value, state.snapshot)
                NoStateChange
            },
        )
    }

    public fun <T> collectWhileInStateEffect(
        flowBuilder: (state: Flow<InputState>) -> Flow<T>,
        executionPolicy: ExecutionPolicy = ExecutionPolicy.ORDERED,
        handler: suspend (item: T, state: InputState) -> Unit,
    ) {
        collectWhileInState(
            flowBuilder = flowBuilder,
            executionPolicy = executionPolicy,
            handler = { value: T, state: State<InputState> ->
                handler(value, state.snapshot)
                NoStateChange
            },
        )
    }

    public fun <SubStateMachineState : Any> onEnterStartStateMachine(
        stateMachine: StateMachine<SubStateMachineState, A>,
        stateMapper: (State<InputState>, SubStateMachineState) -> ChangedState<S> = { _, subState ->
            @Suppress("UNCHECKED_CAST")
            OverrideState(subState as S)
        },
    ) {
        onEnterStartStateMachine(
            stateMachineFactory = { stateMachine },
            actionMapper = { it },
            stateMapper = stateMapper,
        )
    }

    public fun <SubStateMachineState : Any> onEnterStartStateMachine(
        stateMachineFactory: (InputState) -> StateMachine<SubStateMachineState, A>,
        stateMapper: (State<InputState>, SubStateMachineState) -> ChangedState<S> = { _, subState ->
            @Suppress("UNCHECKED_CAST")
            OverrideState(subState as S)
        },
    ) {
        onEnterStartStateMachine(
            stateMachineFactory = stateMachineFactory,
            actionMapper = { it },
            stateMapper = stateMapper,
        )
    }

    public fun <SubStateMachineState : Any, SubStateMachineAction : Any> onEnterStartStateMachine(
        stateMachine: StateMachine<SubStateMachineState, SubStateMachineAction>,
        actionMapper: (A) -> SubStateMachineAction?,
        stateMapper: (State<InputState>, SubStateMachineState) -> ChangedState<S> = { _, subState ->
            @Suppress("UNCHECKED_CAST")
            OverrideState(subState as S)
        },
    ) {
        onEnterStartStateMachine(
            stateMachineFactory = { stateMachine },
            actionMapper = actionMapper,
            stateMapper = stateMapper,
        )
    }

    public fun <SubStateMachineState : Any, SubStateMachineAction : Any> onEnterStartStateMachine(
        stateMachineFactory: (InputState) -> StateMachine<SubStateMachineState, SubStateMachineAction>,
        actionMapper: (A) -> SubStateMachineAction?,
        stateMapper: (State<InputState>, SubStateMachineState) -> ChangedState<S> = { _, subState ->
            @Suppress("UNCHECKED_CAST")
            OverrideState(subState as S)
        },
    ) {
        _inStateSideEffectBuilders.add(
            StartStatemachineOnEnterSideEffectBuilder(
                subStateMachineFactory = stateMachineFactory,
                actionMapper = actionMapper,
                stateMapper = stateMapper,
                isInState = _isInState,
            ),
        )
    }

    public inline fun <reified SubAction : A, SubStateMachineState : Any> onActionStartStateMachine(
        stateMachine: StateMachine<SubStateMachineState, A>,
        noinline stateMapper: (State<InputState>, SubStateMachineState) -> ChangedState<S>,
    ) {
        onActionStartStateMachine(
            stateMachineFactory = { _: SubAction, _: InputState -> stateMachine },
            actionMapper = { it },
            stateMapper = stateMapper,
        )
    }

    public inline fun <reified SubAction : A, SubStateMachineState : Any> onActionStartStateMachine(
        noinline stateMachineFactory: (SubAction, InputState) -> StateMachine<SubStateMachineState, A>,
        noinline stateMapper: (State<InputState>, SubStateMachineState) -> ChangedState<S>,
    ) {
        onActionStartStateMachine(
            stateMachineFactory = stateMachineFactory,
            actionMapper = { it },
            stateMapper = stateMapper,
        )
    }

    public inline fun <reified SubAction : A, SubStateMachineState : Any, SubStateMachineAction : Any> onActionStartStateMachine(
        noinline stateMachineFactory: (SubAction, InputState) -> StateMachine<SubStateMachineState, SubStateMachineAction>,
        noinline actionMapper: (A) -> SubStateMachineAction?,
        noinline stateMapper: (State<InputState>, SubStateMachineState) -> ChangedState<S>,
    ) {
        onActionStartStateMachine(
            actionClass = SubAction::class,
            stateMachineFactory = stateMachineFactory,
            actionMapper = actionMapper,
            stateMapper = stateMapper,
        )
    }

    @PublishedApi
    internal fun <SubAction : A, SubStateMachineState : Any, SubStateMachineAction : Any> onActionStartStateMachine(
        actionClass: KClass<out SubAction>,
        stateMachineFactory: (SubAction, InputState) -> StateMachine<SubStateMachineState, SubStateMachineAction>,
        actionMapper: (A) -> SubStateMachineAction?,
        stateMapper: (State<InputState>, SubStateMachineState) -> ChangedState<S>,
    ) {
        val builder = StartStateMachineOnActionInStateSideEffectBuilder<SubStateMachineState, SubStateMachineAction, InputState, SubAction, S, A>(
            subStateMachineFactory = stateMachineFactory,
            actionMapper = actionMapper,
            stateMapper = stateMapper,
            isInState = _isInState,
            subActionClass = actionClass,
        )

        _inStateSideEffectBuilders.add(builder)
    }
}
