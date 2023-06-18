package com.example.statemachine.dsl.util

internal expect class AtomicCounter(initialValue: Int) {
    internal fun get(): Int
    internal fun incrementAndGet(): Int
    internal fun decrementAndGet(): Int
}