package com.example.statemachine.dsl.util

import com.example.statemachine.dsl.Action
import com.example.statemachine.dsl.GetState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

internal fun <S, A> Flow<Action<S, A>>.whileInState(
    isInState: (S) -> Boolean,
    getState: GetState<S>,
    transform: suspend (Flow<Action<S, A>>) -> Flow<Action<S, A>>,
) = channelFlow {
    var currentChannel: Channel<Action<S, A>>? = null

    collect { value ->
        if (isInState(getState())) {

            if (currentChannel == null) {
                currentChannel = Channel()
                launch {
                    transform(currentChannel!!.consumeAsFlow())
                        .collect { send(it) }
                }
            }
            currentChannel!!.send(value)
        } else {
            currentChannel?.close(CancellationException("StateMachine left the state"))
            currentChannel = null
        }
    }
}
