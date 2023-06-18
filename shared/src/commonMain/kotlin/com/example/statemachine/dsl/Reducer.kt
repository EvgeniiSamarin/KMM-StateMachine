package com.example.statemachine.dsl

/**
 * @param S The type of the state
 * @param A The type of the Actions
 */
internal typealias Reducer<S, A> = (S, A) -> S
