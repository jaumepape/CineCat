import SwiftUI

// Punt d'entrada de l'app iOS. Tota la interfície és Compose Multiplatform
// (mòdul `shared`); Swift només la munta dins una finestra.
@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
