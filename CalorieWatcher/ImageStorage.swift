import Foundation
import UIKit

struct ImageStorage {
    static let shared = ImageStorage()
    
    private var documentsDirectory: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
    }
    
    func save(_ data: Data, id: UUID) {
        let url = documentsDirectory.appendingPathComponent("\(id).jpg")
        try? data.write(to: url)
    }
    
    func load(id: UUID) -> UIImage? {
        let url = documentsDirectory.appendingPathComponent("\(id).jpg")
        if let data = try? Data(contentsOf: url) {
            return UIImage(data: data)
        }
        return nil
    }
    
    func delete(id: UUID) {
        let url = documentsDirectory.appendingPathComponent("\(id).jpg")
        try? FileManager.default.removeItem(at: url)
    }
}
