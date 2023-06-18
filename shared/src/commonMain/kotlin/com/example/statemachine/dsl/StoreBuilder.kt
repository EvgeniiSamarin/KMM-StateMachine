package com.example.statemachine.dsl

import kotlin.reflect.KClass
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@FlowReduxDsl
public class StoreBuilder<S : Any, A : Any> {

    private val builderBlocks: MutableList<InStateBuilderBlock<*, S, A>> = ArrayList()

    /**
     * получение событий если находимся в этом состоянии
     */
    public inline fun <reified SubState : S> inState(
        noinline block: InStateBuilderBlock<SubState, S, A>.() -> Unit,
    ) {
        inState(SubState::class, block)
    }

    @PublishedApi
    internal fun <SubState : S> inState(
        subStateClass: KClass<SubState>,
        block: InStateBuilderBlock<SubState, S, A>.() -> Unit,
    ) {
        val builder = InStateBuilderBlock<SubState, S, A>(_isInState = { state ->
            subStateClass.isInstance(state)
        })
        block(builder)
        builderBlocks.add(builder)
    }

    public inline fun <reified SubState : S> inState(
        noinline additionalIsInState: (SubState) -> Boolean,
        noinline block: InStateBuilderBlock<SubState, S, A>.() -> Unit,
    ) {
        inState(SubState::class, additionalIsInState, block)
    }

    @PublishedApi
    internal fun <SubState : S> inState(
        subStateClass: KClass<SubState>,
        additionalIsInState: (SubState) -> Boolean,
        block: InStateBuilderBlock<SubState, S, A>.() -> Unit,
    ) {
        val builder = InStateBuilderBlock<SubState, S, A>(_isInState = { state ->
            @Suppress("UNCHECKED_CAST")
            subStateClass.isInstance(state) && additionalIsInState(state as SubState)
        })
        block(builder)
        builderBlocks.add(builder)
    }

    public fun inStateWithCondition(
        isInState: (S) -> Boolean,
        block: InStateBuilderBlock<S, S, A>.() -> Unit,
    ) {
        val builder = InStateBuilderBlock<S, S, A>(_isInState = isInState)
        block(builder)
        builderBlocks.add(builder)
    }

    internal fun generateSideEffects(): List<SideEffect<S, Action<S, A>>> =
        builderBlocks.flatMap { builder ->
            builder.generateSideEffects()
        }
}
