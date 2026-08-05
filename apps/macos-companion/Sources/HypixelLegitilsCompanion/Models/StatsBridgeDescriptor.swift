import Foundation

struct StatsBridgeDescriptor: Codable, Equatable {
    static let schemaVersion = 1

    let schemaVersion: Int
    let port: UInt16
    let capability: String
    let expiresAtEpochMillis: Int64

    init(schemaVersion: Int, port: UInt16, capability: String, expiresAt: Date) {
        self.schemaVersion = schemaVersion
        self.port = port
        self.capability = capability
        expiresAtEpochMillis = Int64(expiresAt.timeIntervalSince1970 * 1_000)
    }

    func isUsable(at date: Date = .now) -> Bool {
        schemaVersion == Self.schemaVersion
            && port > 0
            && !capability.isEmpty
            && expiresAtEpochMillis > Int64(date.timeIntervalSince1970 * 1_000)
    }
}
