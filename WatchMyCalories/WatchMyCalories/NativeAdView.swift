import GoogleMobileAds
import os
import SwiftUI

// MARK: - Native Ad Loader

final class NativeAdLoader: NSObject, ObservableObject, NativeAdLoaderDelegate {
    private let logger = Logger(subsystem: "com.pning80.WatchMyCalories", category: "NativeAd")
    @Published var nativeAd: NativeAd?
    private var adLoader: AdLoader?

    func loadAd() {
        guard !AdManager.isUITestingMode else { return }
        guard AdManager.shared.canRequestAds else { return }

        let mediaOptions = NativeAdMediaAdLoaderOptions()

        let videoOptions = VideoOptions()
        videoOptions.shouldStartMuted = true
        videoOptions.isClickToExpandRequested = true

        let loader = AdLoader(
            adUnitID: AdManager.nativeAdUnitID,
            rootViewController: nil,
            adTypes: [.native],
            options: [mediaOptions, videoOptions]
        )
        loader.delegate = self
        self.adLoader = loader
        loader.load(GoogleMobileAds.Request())
    }

    func adLoader(_ adLoader: AdLoader, didReceive nativeAd: NativeAd) {
        self.nativeAd = nativeAd
    }

    func adLoader(_ adLoader: AdLoader, didFailToReceiveAdWithError error: Error) {
        logger.error("Failed to load: \(error.localizedDescription)")
    }
}

// MARK: - Native Ad View

struct NativeAdContentView: UIViewRepresentable {
    let nativeAd: NativeAd

    private static let mediaHeightTag = 999

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

        // Icon
        let iconView = UIImageView()
        iconView.contentMode = .scaleAspectFill
        iconView.clipsToBounds = true
        iconView.layer.cornerRadius = 8
        iconView.translatesAutoresizingMaskIntoConstraints = false
        adView.iconView = iconView

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

        // Advertiser + star rating + store row
        let infoStack = UIStackView()
        infoStack.axis = .horizontal
        infoStack.spacing = 8
        infoStack.alignment = .center
        infoStack.translatesAutoresizingMaskIntoConstraints = false

        let advertiserLabel = UILabel()
        advertiserLabel.font = UIFont.systemFont(ofSize: 11)
        advertiserLabel.textColor = .tertiaryLabel
        advertiserLabel.translatesAutoresizingMaskIntoConstraints = false
        adView.advertiserView = advertiserLabel

        let starLabel = UILabel()
        starLabel.font = UIFont.systemFont(ofSize: 11)
        starLabel.textColor = .tertiaryLabel
        starLabel.translatesAutoresizingMaskIntoConstraints = false
        adView.starRatingView = starLabel

        let storeLabel = UILabel()
        storeLabel.font = UIFont.systemFont(ofSize: 11)
        storeLabel.textColor = .tertiaryLabel
        storeLabel.translatesAutoresizingMaskIntoConstraints = false
        adView.storeView = storeLabel

        infoStack.addArrangedSubview(advertiserLabel)
        infoStack.addArrangedSubview(starLabel)
        infoStack.addArrangedSubview(storeLabel)

        // Call to action
        var ctaConfig = UIButton.Configuration.filled()
        ctaConfig.contentInsets = NSDirectionalEdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16)
        ctaConfig.baseBackgroundColor = UIColor(red: 0.18, green: 0.42, blue: 0.31, alpha: 1.0)
        ctaConfig.baseForegroundColor = .white
        ctaConfig.cornerStyle = .medium
        ctaConfig.titleTextAttributesTransformer = UIConfigurationTextAttributesTransformer { incoming in
            var outgoing = incoming
            outgoing.font = UIFont.systemFont(ofSize: 14, weight: .semibold)
            return outgoing
        }
        let ctaButton = UIButton(configuration: ctaConfig)
        ctaButton.isUserInteractionEnabled = false
        ctaButton.translatesAutoresizingMaskIntoConstraints = false
        adView.callToActionView = ctaButton

        // Layout
        adView.addSubview(mediaView)
        adView.addSubview(adBadge)
        adView.addSubview(iconView)
        adView.addSubview(headlineLabel)
        adView.addSubview(bodyLabel)
        adView.addSubview(infoStack)
        adView.addSubview(ctaButton)

        let mediaHeight = mediaView.heightAnchor.constraint(equalTo: adView.widthAnchor, multiplier: 9.0 / 16.0)
        mediaHeight.identifier = "mediaHeight"

        NSLayoutConstraint.activate([
            mediaView.topAnchor.constraint(equalTo: adView.topAnchor),
            mediaView.leadingAnchor.constraint(equalTo: adView.leadingAnchor),
            mediaView.trailingAnchor.constraint(equalTo: adView.trailingAnchor),
            mediaHeight,

            adBadge.topAnchor.constraint(equalTo: mediaView.topAnchor, constant: 8),
            adBadge.leadingAnchor.constraint(equalTo: mediaView.leadingAnchor, constant: 8),
            adBadge.widthAnchor.constraint(equalToConstant: 28),
            adBadge.heightAnchor.constraint(equalToConstant: 18),

            iconView.topAnchor.constraint(equalTo: mediaView.bottomAnchor, constant: 12),
            iconView.leadingAnchor.constraint(equalTo: adView.leadingAnchor, constant: 16),
            iconView.widthAnchor.constraint(equalToConstant: 40),
            iconView.heightAnchor.constraint(equalToConstant: 40),

            headlineLabel.topAnchor.constraint(equalTo: mediaView.bottomAnchor, constant: 12),
            headlineLabel.leadingAnchor.constraint(equalTo: iconView.trailingAnchor, constant: 12),
            headlineLabel.trailingAnchor.constraint(equalTo: adView.trailingAnchor, constant: -16),

            bodyLabel.topAnchor.constraint(equalTo: headlineLabel.bottomAnchor, constant: 4),
            bodyLabel.leadingAnchor.constraint(equalTo: headlineLabel.leadingAnchor),
            bodyLabel.trailingAnchor.constraint(equalTo: headlineLabel.trailingAnchor),

            infoStack.topAnchor.constraint(equalTo: bodyLabel.bottomAnchor, constant: 8),
            infoStack.leadingAnchor.constraint(equalTo: adView.leadingAnchor, constant: 16),
            infoStack.trailingAnchor.constraint(lessThanOrEqualTo: adView.trailingAnchor, constant: -16),

            ctaButton.topAnchor.constraint(equalTo: infoStack.bottomAnchor, constant: 12),
            ctaButton.leadingAnchor.constraint(equalTo: adView.leadingAnchor, constant: 16),
            ctaButton.bottomAnchor.constraint(equalTo: adView.bottomAnchor, constant: -12),
        ])

        return adView
    }

    func updateUIView(_ adView: GoogleMobileAds.NativeAdView, context: Context) {
        adView.nativeAd = nativeAd

        // Media
        adView.mediaView?.mediaContent = nativeAd.mediaContent
        let aspectRatio = nativeAd.mediaContent.aspectRatio
        if aspectRatio > 0 {
            if let existing = adView.constraints.first(where: { $0.identifier == "mediaHeight" }) {
                existing.isActive = false
            }
            let updated = adView.mediaView!.heightAnchor.constraint(
                equalTo: adView.widthAnchor, multiplier: 1.0 / aspectRatio
            )
            updated.identifier = "mediaHeight"
            updated.isActive = true
        }

        // Icon
        if let icon = nativeAd.icon {
            (adView.iconView as? UIImageView)?.image = icon.image
            adView.iconView?.isHidden = false
        } else {
            adView.iconView?.isHidden = true
        }

        // Headline & body
        (adView.headlineView as? UILabel)?.text = nativeAd.headline
        (adView.bodyView as? UILabel)?.text = nativeAd.body

        // Adjust headline leading when icon is hidden
        if let headlineLabel = adView.headlineView {
            let iconHidden = adView.iconView?.isHidden ?? true
            if let leadingConstraint = headlineLabel.superview?.constraints.first(where: {
                $0.firstItem === headlineLabel && $0.firstAttribute == .leading
            }) {
                if iconHidden {
                    leadingConstraint.isActive = false
                    headlineLabel.leadingAnchor.constraint(equalTo: adView.leadingAnchor, constant: 16).isActive = true
                }
            }
        }

        // Advertiser
        (adView.advertiserView as? UILabel)?.text = nativeAd.advertiser
        adView.advertiserView?.isHidden = nativeAd.advertiser == nil

        // Star rating
        if let rating = nativeAd.starRating {
            (adView.starRatingView as? UILabel)?.text = "★ \(rating)"
            adView.starRatingView?.isHidden = false
        } else {
            adView.starRatingView?.isHidden = true
        }

        // Store
        (adView.storeView as? UILabel)?.text = nativeAd.store
        adView.storeView?.isHidden = nativeAd.store == nil

        // Call to action
        (adView.callToActionView as? UIButton)?.setTitle(nativeAd.callToAction, for: .normal)
        adView.callToActionView?.isHidden = nativeAd.callToAction == nil
    }
}
