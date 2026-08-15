import Foundation
import Security

enum KeychainStoreError: LocalizedError {
    case operationFailed(OSStatus)

    var errorDescription: String? {
        switch self {
        case .operationFailed(let status):
            "Keychain operation failed (\(status))."
        }
    }
}

protocol StatsProviderKeyReading {
    func readSecret(account: String) throws -> String?
}

struct KeychainStore {
    /// A stable signed-app namespace. Legacy unsigned builds used `legacyService` and
    /// can leave entries that this app is intentionally unable to read.
    static let service = "com.snkisk.hypixellegitils.companion.v2"
    static let legacyService = "com.snkisk.hypixellegitils.companion"

    static func needsLegacyReentry(for provider: StatsProvider, hasCurrent: Bool, hasLegacy: Bool) -> Bool {
        provider.requiresAPIKey && !hasCurrent && hasLegacy
    }

    func save(secret: String, account: String) throws {
        let data = Data(secret.utf8)
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Self.service,
            kSecAttrAccount as String: account
        ]
        SecItemDelete(query as CFDictionary)
        var add = query
        add[kSecValueData as String] = data
        add[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
        let result = SecItemAdd(add as CFDictionary, nil)
        guard result == errSecSuccess else { throw KeychainStoreError.operationFailed(result) }
    }

    func hasSecret(account: String) -> Bool {
        hasItem(account: account, service: Self.service)
    }

    /// Presence-only probe used to explain a one-time re-entry. It does not request data.
    func hasLegacySecret(account: String) -> Bool {
        hasItem(account: account, service: Self.legacyService)
    }

    private func hasItem(account: String, service: String) -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: false,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        return SecItemCopyMatching(query as CFDictionary, nil) == errSecSuccess
    }

    /// This value is intentionally scoped to the Companion process. Callers must never
    /// put it in configuration, bridge responses, logs, or observable UI state.
    func readSecret(account: String) throws -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Self.service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound { return nil }
        guard status == errSecSuccess,
              let data = result as? Data,
              let secret = String(data: data, encoding: .utf8) else {
            throw KeychainStoreError.operationFailed(status)
        }
        return secret
    }

    func remove(account: String) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Self.service,
            kSecAttrAccount as String: account
        ]
        let result = SecItemDelete(query as CFDictionary)
        guard result == errSecSuccess || result == errSecItemNotFound else {
            throw KeychainStoreError.operationFailed(result)
        }
    }
}

extension KeychainStore: StatsProviderKeyReading {}
