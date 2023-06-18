package com.example.statemachine.dsl

import kotlinx.coroutines.flow.Flow

internal typealias SideEffect<S, A> = (actions: Flow<A>, getState: GetState<S>) -> Flow<A>
internal typealias GetState<S> = () -> S
