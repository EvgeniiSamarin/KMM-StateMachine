import SwiftUI
import shared

struct TodoReposList: View {
    let contentState: ShowContentPaginationState
    let dispatchAction: (Action) -> Void

    var body: some View {
        List {
            ForEach(contentState.items) { repo in
                TodoRepositoryRow(repo: repo, dispatchAction: dispatchAction)
            }

            nextLoadingPageStatusView()
                .frame(maxWidth: .infinity)
        }
    }

    @ViewBuilder
    private func nextLoadingPageStatusView() -> some View {
        switch contentState.nextPageLoadingState {
        case .loading:
            HStack(alignment: .center) {
                LoadingIndicatorView(style: .small)
            }
        case .error:
            ErrorView { dispatchAction(LoadNextPage()) }
        case .idle:
            Rectangle()
                .size(width: 0, height: 0)
                .onAppear(perform: { dispatchAction( LoadNextPage() ) })
        default:
            fatalError("Unhandled case: \(contentState.nextPageLoadingState)")

        }
    }
}

struct TodoReposList_Previews: PreviewProvider {
    static var previews: some View {
        VStack {
            view(forLoadingState: .loading)
            view(forLoadingState: .error)
            view(forLoadingState: .idle)
        }
    }

    private static func view(forLoadingState loadingState: NextPageLoadingState) -> TodoReposList {
        TodoReposList(contentState: .init(items: [TodoRepository(id: "1",
                                                                     name: "repo name",
                                                                     stargazersCount: 123,
                                                                     favoriteStatus: .notFavorite)],
                                            nextPageLoadingState: loadingState,
                                            currentPage: 1,
                                            canLoadNextPage: true),
                        dispatchAction: { _ in })
    }
}

extension TodoRepository: Identifiable {
}
