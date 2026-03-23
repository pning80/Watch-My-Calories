import GoogleMobileAds
import os
import SwiftUI

struct BannerAdView: View {
    @ObservedObject private var adManager = AdManager.shared
    @State private var adHeight: CGFloat = 0
    @State private var reloadTrigger: Bool = false

    var body: some View {
        Group {
            if !AdManager.isUITestingMode && adManager.canRequestAds {
                BannerAdRepresentable(adHeight: $adHeight, reloadTrigger: reloadTrigger)
                    .frame(maxWidth: .infinity)
                    .frame(height: adHeight)
                    .clipped()
                    .opacity(adHeight > 0 ? 1 : 0)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)
                    .padding(.horizontal)
                    .animation(.easeInOut(duration: 0.3), value: adHeight)
                    .accessibilityIdentifier(AccessibilityID.Ads.banner)
                    .onAppear {
                        reloadTrigger.toggle()
                    }
            }
        }
    }
}

private struct BannerAdRepresentable: UIViewRepresentable {
    @Binding var adHeight: CGFloat
    var reloadTrigger: Bool

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    func makeUIView(context: Context) -> BannerView {
        let bannerView = BannerView()
        bannerView.backgroundColor = .clear
        bannerView.clipsToBounds = true
        bannerView.isHidden = true
        bannerView.adUnitID = AdManager.bannerAdUnitID
        bannerView.delegate = context.coordinator
        bannerView.setContentHuggingPriority(.defaultLow, for: .vertical)
        bannerView.setContentCompressionResistancePriority(.defaultLow, for: .vertical)
        context.coordinator.bannerView = bannerView

        DispatchQueue.main.async {
            context.coordinator.loadAdIfNeeded(bannerView)
        }

        return bannerView
    }

    func updateUIView(_ uiView: BannerView, context: Context) {
        context.coordinator.parent = self
        if reloadTrigger != context.coordinator.lastReloadTrigger {
            context.coordinator.lastReloadTrigger = reloadTrigger
            let shouldReload: Bool
            if !context.coordinator.hasEverReceivedAd {
                shouldReload = true
            } else if !context.coordinator.isAdValid {
                shouldReload = true
            } else if let lastTime = context.coordinator.lastAdReceivedTime,
                      Date().timeIntervalSince(lastTime) > Coordinator.maxAdAge {
                shouldReload = true
            } else {
                shouldReload = false
            }
            if shouldReload {
                uiView.isHidden = true
                context.coordinator.retryCount = 0
                context.coordinator.loadAdIfNeeded(uiView)
            }
        }
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: BannerView, context: Context) -> CGSize? {
        return CGSize(width: proposal.width ?? UIScreen.main.bounds.width, height: adHeight)
    }

    class Coordinator: NSObject, BannerViewDelegate {
        private let logger = Logger(subsystem: "com.pning80.WatchMyCalories", category: "BannerAd")
        var parent: BannerAdRepresentable
        weak var bannerView: BannerView?
        var retryCount = 0
        var hasEverReceivedAd = false
        var isAdValid = false
        var lastAdReceivedTime: Date?
        var lastReloadTrigger: Bool = false
        private static let maxRetries = 3
        static let maxAdAge: TimeInterval = 150

        init(parent: BannerAdRepresentable) {
            self.parent = parent
        }

        func loadAdIfNeeded(_ bannerView: BannerView) {
            guard let windowScene = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .first,
                let rootVC = windowScene.windows.first?.rootViewController else {
                DispatchQueue.main.async { [weak self] in
                    self?.loadAdIfNeeded(bannerView)
                }
                return
            }
            bannerView.rootViewController = rootVC
            let width = rootVC.view.frame.width - 32
            bannerView.adSize = currentOrientationAnchoredAdaptiveBanner(width: width)
            bannerView.load(GoogleMobileAds.Request())
        }

        func bannerViewDidReceiveAd(_ bannerView: BannerView) {
            retryCount = 0
            hasEverReceivedAd = true
            isAdValid = true
            lastAdReceivedTime = Date()
            bannerView.isHidden = false
            let height = min(bannerView.adSize.size.height, 90)
            parent.adHeight = height
        }

        func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
            logger.error("Failed to load: \(error.localizedDescription)")
            isAdValid = false
            // Only hide if no ad was ever successfully loaded — keep showing last good ad
            if !hasEverReceivedAd {
                bannerView.isHidden = true
                parent.adHeight = 0
            }

            guard retryCount < Self.maxRetries else { return }
            retryCount += 1
            let delay = pow(2.0, Double(retryCount))
            DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [weak self] in
                guard let self, let bannerView = self.bannerView else { return }
                bannerView.load(GoogleMobileAds.Request())
            }
        }
    }
}
