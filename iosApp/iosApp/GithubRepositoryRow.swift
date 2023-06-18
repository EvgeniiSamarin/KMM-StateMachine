import SwiftUI
import shared

struct TodoRepositoryRow: View {
    let repo: TodoRepository
    let dispatchAction: (Action) -> Void


    var body: some View {
        HStack {
            Text(repo.name)
            
            Spacer()

            Text("\(repo.stargazersCount)")
            
            favoriteStatusView()
                .frame(width: 16, height: 16)
        }
    }

    @ViewBuilder
    private func favoriteStatusView() -> some View {
        switch repo.favoriteStatus {
        case .notFavorite:
            Image(systemName: "heart.fill")
                .onTapGesture {
                    dispatchAction(ToggleFavoriteAction(id: repo.id))
                }
            
        case .favorite:
            Image(systemName: "heart.fill")
                .onTapGesture {
                    dispatchAction(ToggleFavoriteAction(id: repo.id))
                }
            
        case .operationFailed:
            Image(systemName: "exclamationmark.circle")
                .onTapGesture {
                    dispatchAction(RetryToggleFavoriteAction(id: repo.id))
                }
            
            
        case .operationInProgress:
            LoadingIndicatorView(style: .verySmall)
            
        default:
            fatalError("Unhandled case: \(repo.favoriteStatus)")
        }
    }
}

struct TodoRepositoryRow_Previews: PreviewProvider {
    static var previews: some View {
        VStack {
            view(forFavoriteStatus: .favorite)
            view(forFavoriteStatus: .notFavorite)
            view(forFavoriteStatus: .operationInProgress)
            view(forFavoriteStatus: .operationFailed)
        }
    }

    private static func view(forFavoriteStatus favoriteStatus: FavoriteStatus) -> TodoRepositoryRow {
        TodoRepositoryRow(
            repo: TodoRepository(id: "some_id", name: "Github Repo name", stargazersCount: 23, favoriteStatus: favoriteStatus),
            dispatchAction: { _ in }
        )
    }
}
