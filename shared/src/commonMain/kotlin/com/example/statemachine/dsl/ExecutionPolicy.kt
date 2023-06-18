package com.example.statemachine.dsl

/**
 * Правила получения событий в сторе
 */
public enum class ExecutionPolicy {
    CANCEL_PREVIOUS,
    UNORDERED,
    ORDERED,
}
