package com.example.statemachine.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.freeletics.flowredux.sample.shared.TodoApi
import com.freeletics.flowredux.sample.shared.InternalPaginationStateMachine

class ComposeActivity : ComponentActivity() {

    private val stateMachine = InternalPaginationStateMachine(todoApi = TodoApi())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val (state, dispatch) = stateMachine.rememberStateAndDispatch()
            PopularTodoUi(state.value, dispatch)
        }
    }
}
