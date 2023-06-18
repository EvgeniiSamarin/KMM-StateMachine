package com.example.statemachine.dsl.util

import com.example.statemachine.dsl.ExecutionPolicy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge

@ExperimentalCoroutinesApi
internal fun <T, R> Flow<T>.flatMapWithExecutionPolicy(
    executionPolicy: ExecutionPolicy,
    transform: suspend (value: T) -> Flow<R>,
): Flow<R> =
    when (executionPolicy) {
        ExecutionPolicy.CANCEL_PREVIOUS -> this.flatMapLatest(transform)
        ExecutionPolicy.ORDERED -> this.flatMapConcat(transform)
        ExecutionPolicy.UNORDERED -> this.flatMapMerge(transform = transform)
    }
