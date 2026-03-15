import SwiftUI
import GoogleMobileAds

// MARK: - Native Ad Loader

final class NativeAdLoader: NSObject, ObservableObject, NativeAdLoaderDelegate {
    @Published var nativeAd: NativeAd?
    private var adLoader: AdLoader?

    func loadAd() {
        guard !AdManager.isUITestingMode else { return }
        guard AdManager.shared.canRequestAds else { return }

        let loader = AdLoader(
            adUnitID: AdManager.nativeAdUnitID,
            rootViewController: nil,
            adTypes: [.native],
            options: nil
        )
        loader.delegate = self
        self.adLoader = loader
        loader.load(GoogleMobileAds.Request())
    }

    func adLoader(_ adLoader: AdLoader, didReceive nativeAd: NativeAd) {
        self.nativeAd = nativeAd
    }

    func adLoader(_ adLoader: AdLoader, didFailToReceiveAdWithError error: Error) {
        print("[NativeAd] Failed to load: \(error.localizedDescription)")
    }
}

// MARK: - Native Ad View

struct NativeAdContentView: UIViewRepresentable {
    let nativeAd: NativeAd

    func makeUIView(context: Context) -> GoogleMobileAds.NativeAdView {
        let adView = GoogleMobileAds.NativeAdView()
        adView.backgroundColor = UIColor.secondarySystemBackground
        adView.layer.cornerRadius = 16
        adView.clipsToBounds = true

        // Ad badge
        let adBadge = UILabel()
        adBadge.text = "Ad"
        adBadge.font = UIFont.systemFont(ofSize: 11, weight: .bold)
        adBadge.textColor = .white
        adBadge.backgroundColor = UIColor(red: 0.18, green: 0.42, blue: 0.31, alpha: 1.0)
        adBadge.textAlignment = .center
        adBadge.layer.cornerRadius = 4
        adBadge.clipsToBounds = true
        adBadge.translatesAutoresizingMaskIntoConstraints = false

        // Media view
        let mediaView = MediaView()
        mediaView.contentMode = .scaleAspectFill
        mediaView.clipsToBounds = true
        mediaView.translatesAutoresizingMaskIntoConstraints = false
        adView.mediaView = mediaView

        // Headline
        let headlineLabel = UILabel()
        headlineLabel.font = UIFont.systemFont(ofSize: 16, weight: .semibold)
        headlineLabel.textColor = .label
        headlineLabel.numberOfLines = 2
        headlineLabel.translatesAutoresizingMaskIntoConstraints = false
        adView.headlineView = headlineLabel

        // Body
        let bodyLabel = UILabel()
        bodyLabel.font = UIFont.systemFont(ofSize: 13)
        bodyLabel.textColor = .secondaryLabel
        bodyLabel.numberOfLines = 3
        bodyLabel.translatesAutoresizingMaskIntoConstraints = false
        adView.bodyView = bodyLabel

        // Call to action
        let ctaButton = UIButton(type: .system)
        ctaButton.titleLabel?.font = UIFont.systemFont(ofSize: 14, weight: .semibold)
        ctaButton.setTitleColor(.white, for: .normal)
        ctaButton.backgroundColor = UIColor(red: 0.18, green: 0.42, blue: 0.31, alpha: 1.0)
        ctaButton.layer.cornerRadius = 8
        ctaButton.contentEdgeInsets = UIEdgeInsets(top: 8, left: 16, bottom: 8, right: 16)
        ctaButton.isUserInteractionEnabled = false
        ctaButton.translatesAutoresizingMaskIntoConstraints = false
        adView.callToActionView = ctaButton

        // Layout
        adView.addSubview(mediaView)
        adView.addSubview(adBadge)
        adView.addSubview(headlineLabel)
        adView.addSubview(bodyLabel)
        adView.addSubview(ctaButton)

        NSLayoutConstraint.activate([
            mediaView.topAnchor.constraint(equalTo: adView.topAnchor),
            mediaView.leadingAnchor.constraint(equalTo: adView.leadingAnchor),
            mediaView.trailingAnchor.constraint(equalTo: adView.trailingAnchor),
            mediaView.heightAnchor.constraint(equalTo: adView.widthAnchor, multiplier: 9.0 / 16.0),

            adBadge.topAnchor.constraint(equalTo: mediaView.topAnchor, constant: 8),
            adBadge.leadingAnchor.constraint(equalTo: mediaView.leadingAnchor, constant: 8),
            adBadge.widthAnchor.constraint(equalToConstant: 28),
            adBadge.heightAnchor.constraint(equalToConstant: 18),

            headlineLabel.topAnchor.constraint(equalTo: mediaView.bottomAnchor, constant: 12),
            headlineLabel.leadingAnchor.constraint(equalTo: adView.leadingAnchor, constant: 16),
            headlineLabel.trailingAnchor.constraint(equalTo: adView.trailingAnchor, constant: -16),

            bodyLabel.topAnchor.constraint(equalTo: headlineLabel.bottomAnchor, constant: 4),
            bodyLabel.leadingAnchor.constraint(equalTo: headlineLabel.leadingAnchor),
            bodyLabel.trailingAnchor.constraint(equalTo: headlineLabel.trailingAnchor),

            ctaButton.topAnchor.constraint(equalTo: bodyLabel.bottomAnchor, constant: 12),
            ctaButton.leadingAnchor.constraint(equalTo: headlineLabel.leadingAnchor),
            ctaButton.bottomAnchor.constraint(equalTo: adView.bottomAnchor, constant: -12),
        ])

        return adView
    }

    func updateUIView(_ adView: GoogleMobileAds.NativeAdView, context: Context) {
        adView.nativeAd = nativeAd
        (adView.headlineView as? UILabel)?.text = nativeAd.headline
        (adView.bodyView as? UILabel)?.text = nativeAd.body
        (adView.callToActionView as? UIButton)?.setTitle(nativeAd.callToAction, for: .normal)
        adView.callToActionView?.isHidden = nativeAd.callToAction == nil
        adView.mediaView?.mediaContent = nativeAd.mediaContent
    }
}
