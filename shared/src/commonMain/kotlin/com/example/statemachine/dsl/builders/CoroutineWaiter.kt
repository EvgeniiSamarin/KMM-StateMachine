package com.example.statemachine.dsl.builders

import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlin.jvm.JvmInline

@JvmInline
internal value class CoroutineWaiter(private val job: CompletableJob = Job()) {
    internal suspend inline fun waitUntilResumed() {
        job.join()
    }
    internal fun resume() {
        job.complete()
    }

    internal fun isResumed(): Boolean {
        return job.isCompleted
    }

    internal fun isTheSame(other: CoroutineWaiter): Boolean = job === other.job
}
