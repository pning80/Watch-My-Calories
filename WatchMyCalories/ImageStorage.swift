import Foundation
import UIKit
import os

struct ImageStorage {
    static let shared = ImageStorage()
    private static let logger = Logger(subsystem: Bundle.main.bundleIdentifier ?? "WatchMyCalories", category: "ImageStorage")

    private var documentsDirectory: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
    }

    func save(_ data: Data, id: UUID) -> Bool {
        let url = documentsDirectory.appendingPathComponent("\(id).jpg")
        do {
            try data.write(to: url)
            return true
        } catch {
            Self.logger.error("Failed to save image \(id): \(error.localizedDescription)")
            return false
        }
    }

    func load(id: UUID) -> UIImage? {
        let url = documentsDirectory.appendingPathComponent("\(id).jpg")
        do {
            let data = try Data(contentsOf: url)
            return UIImage(data: data)
        } catch {
            Self.logger.error("Failed to load image \(id): \(error.localizedDescription)")
            return nil
        }
    }

    @discardableResult
    func delete(id: UUID) -> Bool {
        let url = documentsDirectory.appendingPathComponent("\(id).jpg")
        do {
            try FileManager.default.removeItem(at: url)
            return true
        } catch {
            Self.logger.error("Failed to delete image \(id): \(error.localizedDescription)")
            return false
        }
    }
}
