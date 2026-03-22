import SwiftUI
import PhotosUI
import ImageIO

struct PhotoLibraryReviewView: View {
    var onImagesCaptured: ([UIImage], MealType) -> Void
    var onCancel: () -> Void

    @State private var selectedItem: PhotosPickerItem?
    @State private var selectedImage: UIImage?
    @State private var selectedMealType: MealType = MealType.from(date: Date())
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

                    MealTypePicker(selection: $selectedMealType)
                        .padding(.bottom, 16)

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
                            onImagesCaptured([image], selectedMealType)
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
                        let photoDate = extractCreationDate(from: data) ?? Date()
                        selectedMealType = MealType.from(date: photoDate)
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

    private func extractCreationDate(from data: Data) -> Date? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil),
              let props = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [String: Any],
              let exif = props[kCGImagePropertyExifDictionary as String] as? [String: Any],
              let dateStr = exif[kCGImagePropertyExifDateTimeOriginal as String] as? String
        else { return nil }
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyy:MM:dd HH:mm:ss"
        return fmt.date(from: dateStr)
    }
}
