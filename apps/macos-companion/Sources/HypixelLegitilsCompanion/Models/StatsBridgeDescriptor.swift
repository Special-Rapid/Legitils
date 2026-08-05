import Foundation

struct StatsBridgeDescriptor: Codable, Equatable {
    static let schemaVersion = 1

    let schemaVersion: Int
    let port: UInt16
    let capability: String
    let expiresAt: Date

    func isUsable(at date: Date = .now) -> Bool {
        schemaVersion == Self.schemaVersion
            && port > 0
            && !capability.isEmpty
            && expiresAt > date
    }
}
