import SwiftUI
import shared
import os.log

struct ContentView: View {
    private let stateMachine: PaginationStateMachine = PaginationStateMachine(
        todoApi: TodoApi(),
        scope: NsQueueCoroutineScope()
    )

    var body: some View {
        contentView()
            .onAppear(perform: startStateMachine)

    }

    @ViewBuilder
    private func contentView() -> some View {
        switch state {
        case is LoadFirstPagePaginationState:
            LoadingIndicatorView()
        case let state as ShowContentPaginationState:
            GithubReposList(contentState: state, dispatchAction: dispatchAction)
        case is LoadingFirstPageError:
            ErrorView(action: triggerReloadFirstPage)
        default:
            fatalError("Unknown state: \(state.self)")
        }
    }

    private func triggerReloadFirstPage() {
        stateMachine.dispatch(action: RetryLoadingFirstPage())
    }
    
    
    private func dispatchAction(action : Action){
        stateMachine.dispatch(action: action)
    }

    private func startStateMachine() {
        stateMachine.start(stateChangeListener: { (paginationState: PaginationState) -> Void in
            NSLog("Swift UI \(paginationState) to render")
            self.state = paginationState
        })
    }
    
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}

extension String: Error { }
