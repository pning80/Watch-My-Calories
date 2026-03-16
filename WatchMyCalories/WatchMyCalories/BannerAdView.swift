import GoogleMobileAds
import os
import SwiftUI

struct BannerAdView: View {
    @ObservedObject private var adManager = AdManager.shared
    @State private var adHeight: CGFloat = 0

    var body: some View {
        Group {
            if !AdManager.isUITestingMode && adManager.canRequestAds {
                BannerAdRepresentable(adHeight: $adHeight)
                    .frame(maxWidth: .infinity)
                    .frame(height: adHeight)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)
                    .padding(.horizontal)
                    .animation(.easeInOut(duration: 0.3), value: adHeight)
                    .accessibilityIdentifier(AccessibilityID.Ads.banner)
            }
        }
    }
}

private struct BannerAdRepresentable: UIViewRepresentable {
    @Binding var adHeight: CGFloat

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    func makeUIView(context: Context) -> BannerView {
        let bannerView = BannerView()
        bannerView.adUnitID = AdManager.bannerAdUnitID
        bannerView.delegate = context.coordinator
        bannerView.setContentHuggingPriority(.defaultLow, for: .vertical)
        bannerView.setContentCompressionResistancePriority(.defaultLow, for: .vertical)

        DispatchQueue.main.async {
            if let windowScene = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .first,
                let rootVC = windowScene.windows.first?.rootViewController {
                bannerView.rootViewController = rootVC
                let width = rootVC.view.frame.width - 32
                bannerView.adSize = currentOrientationAnchoredAdaptiveBanner(width: width)
                bannerView.load(GoogleMobileAds.Request())
            }
        }

        return bannerView
    }

    func updateUIView(_ uiView: BannerView, context: Context) {}

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: BannerView, context: Context) -> CGSize? {
        return CGSize(width: proposal.width ?? UIScreen.main.bounds.width, height: adHeight)
    }

    class Coordinator: NSObject, BannerViewDelegate {
        private let logger = Logger(subsystem: "com.pning80.WatchMyCalories", category: "BannerAd")
        let parent: BannerAdRepresentable

        init(parent: BannerAdRepresentable) {
            self.parent = parent
        }

        func bannerViewDidReceiveAd(_ bannerView: BannerView) {
            let height = min(bannerView.adSize.size.height, 90)
            parent.adHeight = height
        }

        func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
            logger.error("Failed to load: \(error.localizedDescription)")
        }
    }
}
