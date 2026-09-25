import UIKit
import SwiftUI
import Shared

// Embolcalla el UIViewController que genera Compose Multiplatform
// (shared/src/iosMain/.../MainViewController.kt) perquè SwiftUI el mostri.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
