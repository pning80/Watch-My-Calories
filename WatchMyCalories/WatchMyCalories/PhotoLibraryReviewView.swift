import SwiftUI
import PhotosUI

struct PhotoLibraryReviewView: View {
    var onImagesCaptured: ([UIImage]) -> Void
    var onCancel: () -> Void

    @State private var selectedItem: PhotosPickerItem?
    @State private var selectedImage: UIImage?
    @State private var showPicker = true

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if let image = selectedImage {
                GeometryReader { geometry in
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .frame(width: geometry.size.width, height: geometry.size.height)
                        .clipped()
                }
                .ignoresSafeArea()

                VStack {
                    Spacer()

                    HStack(spacing: 40) {
                        Button(action: {
                            withAnimation {
                                selectedImage = nil
                                selectedItem = nil
                                showPicker = true
                            }
                        }) {
                            Label("Reselect", systemImage: "arrow.counterclockwise")
                                .font(.body)
                                .fontWeight(.semibold)
                                .foregroundStyle(.white)
                                .padding(.horizontal, 24)
                                .padding(.vertical, 14)
                                .background(Capsule().fill(Color.white.opacity(0.25)))
                        }
                        .accessibilityIdentifier(AccessibilityID.PhotoLibrary.chooseAgainButton)

                        Button(action: {
                            onImagesCaptured([image])
                        }) {
                            Label("Use", systemImage: "checkmark")
                                .font(.body)
                                .fontWeight(.semibold)
                                .foregroundStyle(.white)
                                .padding(.horizontal, 24)
                                .padding(.vertical, 14)
                                .background(Capsule().fill(Color.cwAccent))
                                .shadow(radius: 4)
                        }
                        .accessibilityIdentifier(AccessibilityID.PhotoLibrary.usePhotoButton)
                    }
                    .padding(.bottom, 50)
                }
            }
        }
        .photosPicker(isPresented: $showPicker, selection: $selectedItem, matching: .images)
        .onChange(of: selectedItem) { _, newItem in
            guard let newItem else { return }
            newItem.loadTransferable(type: Data.self) { result in
                DispatchQueue.main.async {
                    if case .success(let data) = result, let data, let uiImage = UIImage(data: data) {
                        selectedImage = uiImage
                    } else {
                        selectedItem = nil
                        showPicker = true
                    }
                }
            }
        }
        .onChange(of: showPicker) { _, isShowing in
            if !isShowing && selectedItem == nil {
                onCancel()
            }
        }
    }
}
