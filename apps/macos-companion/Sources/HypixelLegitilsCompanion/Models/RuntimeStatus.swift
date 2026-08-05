import Foundation

struct RuntimeStatus: Codable, Equatable {
    let schemaVersion: Int
    let modVersion: String
    let configRevision: Int64
    let configUsedDefaults: Bool
}
