import XCTest
import UIKit
@testable import WatchMyCalories

final class ImageStorageTests: XCTestCase {

    private let storage = ImageStorage.shared
    private var testIDs: [UUID] = []

    override func tearDownWithError() throws {
        // Clean up any files created during tests
        for id in testIDs {
            storage.delete(id: id)
        }
        testIDs.removeAll()
    }

    private func makeTestID() -> UUID {
        let id = UUID()
        testIDs.append(id)
        return id
    }

    private func makeTestImageData() -> Data {
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: 10, height: 10))
        let image = renderer.image { ctx in
            UIColor.red.setFill()
            ctx.fill(CGRect(x: 0, y: 0, width: 10, height: 10))
        }
        return image.jpegData(compressionQuality: 0.8)!
    }

    // MARK: - Save

    func testSaveReturnsTrueOnSuccess() {
        let id = makeTestID()
        let data = makeTestImageData()
        XCTAssertTrue(storage.save(data, id: id))
    }

    // MARK: - Load

    func testSaveAndLoadRoundTrip() {
        let id = makeTestID()
        let data = makeTestImageData()
        let saved = storage.save(data, id: id)
        XCTAssertTrue(saved)

        let loaded = storage.load(id: id)
        XCTAssertNotNil(loaded)
        XCTAssertGreaterThan(loaded!.size.width, 0)
    }

    func testLoadNonExistentReturnsNil() {
        let id = UUID() // not saved, not tracked for cleanup
        XCTAssertNil(storage.load(id: id))
    }

    // MARK: - Delete

    func testDeleteRemovesFile() {
        let id = makeTestID()
        let data = makeTestImageData()
        let _ = storage.save(data, id: id)

        let deleted = storage.delete(id: id)
        XCTAssertTrue(deleted)
        XCTAssertNil(storage.load(id: id))
    }

    func testDeleteNonExistentReturnsFalse() {
        let id = UUID() // never saved
        XCTAssertFalse(storage.delete(id: id))
    }

    func testDeleteReturnsTrueOnSuccess() {
        let id = makeTestID()
        let _ = storage.save(makeTestImageData(), id: id)
        XCTAssertTrue(storage.delete(id: id))
    }
}
