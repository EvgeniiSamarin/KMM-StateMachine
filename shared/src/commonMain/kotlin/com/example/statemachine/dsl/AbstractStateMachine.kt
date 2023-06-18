package com.example.statemachine.dsl


import com.example.statemachine.dsl.util.AtomicCounter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow

@ExperimentalCoroutinesApi
public abstract class AbstractStateMachine<S : Any, A : Any>(
    private val initialStateSupplier: () -> S,
) : StateMachine<S, A> {

    private val inputActions = Channel<A>()
    private lateinit var outputState: Flow<S>

    private val activeFlowCounter = AtomicCounter(0)

    public constructor(initialState: S) : this(initialStateSupplier = { initialState })

    protected fun spec(specBlock: StoreBuilder<S, A>.() -> Unit) {
        if (::outputState.isInitialized) {
            throw IllegalStateException()
        }

        val sideEffects = StoreBuilder<S, A>().apply(specBlock).generateSideEffects()

        outputState = inputActions
            .receiveAsFlow()
            .map<A, Action<S, A>> { ExternalWrappedAction(it) }
            .onStart {
                emit(InitialStateAction())
            }
            .reduxStore(initialStateSupplier, sideEffects, ::reducer)
            .distinctUntilChanged { old, new -> old === new }
            .onStart {
                if (activeFlowCounter.incrementAndGet() > 1) {
                    throw IllegalStateException()
                }
            }
            .onCompletion {
                activeFlowCounter.decrementAndGet()
            }
    }

    override val state: Flow<S>
        get() {
            checkSpecBlockSet()
            return outputState
        }

    override suspend fun dispatch(action: A) {
        checkSpecBlockSet()
        if (activeFlowCounter.get() <= 0) {
            throw IllegalStateException()
        }
        inputActions.send(action)
    }

    private fun checkSpecBlockSet() {
        if (!::outputState.isInitialized) {
            throw IllegalStateException(
                """
                    No state machine specs are defined. Did you call spec { ... } in init {...}?
                    Example usage:

                    class MyStateMachine : FlowReduxStateMachine<State, Action>(InitialState) {

                        init{
                            spec {
                                inState<FooState> {
                                    on<BarAction> { ... }
                                }
                                ...
                            }
                        }
                    }
                """.trimIndent(),
            )
        }
    }
}
