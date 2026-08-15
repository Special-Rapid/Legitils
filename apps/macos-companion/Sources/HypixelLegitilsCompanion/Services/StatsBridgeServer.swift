import Foundation
import Network
import Security

enum StatsBridgeServerError: LocalizedError {
    case didNotStart
    case unavailablePort
    case descriptorWriteFailed

    var errorDescription: String? {
        switch self {
        case .didNotStart:
            "Stats Bridge を開始できませんでした。"
        case .unavailablePort:
            "Stats Bridge のローカルポートを確保できませんでした。"
        case .descriptorWriteFailed:
            "Stats Bridge の接続情報を保存できませんでした。"
        }
    }
}

/// Small loopback-only HTTP boundary. It accepts one normalized roster request shape and never exposes keys or provider payloads.
final class StatsBridgeServer {
    typealias RosterLookup = (StatsBridgeRosterRequest, @escaping (StatsBridgeRosterResponse) -> Void) -> Void
    typealias HypixelKeyValidation = (@escaping (HypixelAPIKeyValidationStatus) -> Void) -> Void

    private static let descriptorLifetime: TimeInterval = 6 * 60
    private static let descriptorRefreshInterval: TimeInterval = 4 * 60
    private static let maximumRequestBytes = 16 * 1024

    private let queue = DispatchQueue(label: "com.snkisk.hypixellegitils.stats-bridge")
    private let descriptorURL: URL
    private let lookup: RosterLookup
    private let hypixelKeyValidation: HypixelKeyValidation
    private let descriptorLifetime: TimeInterval
    private var listener: NWListener?
    private var descriptor: StatsBridgeDescriptor?
    private var rotationTimer: DispatchSourceTimer?
    private var pendingStartCompletions: [(Result<StatsBridgeDescriptor, Error>) -> Void] = []
    private var availabilityObserver: ((Bool) -> Void)?

    init(
        descriptorURL: URL = CompanionPaths.statsBridgeDescriptorURL,
        lookup: @escaping RosterLookup = { _, completion in completion(.unavailable()) },
        hypixelKeyValidation: @escaping HypixelKeyValidation = { completion in completion(.unavailable) },
        descriptorLifetime: TimeInterval = StatsBridgeServer.descriptorLifetime
    ) {
        self.descriptorURL = descriptorURL
        self.lookup = lookup
        self.hypixelKeyValidation = hypixelKeyValidation
        self.descriptorLifetime = descriptorLifetime
    }

    deinit {
        rotationTimer?.cancel()
        listener?.cancel()
        try? FileManager.default.removeItem(at: descriptorURL)
    }

    func start(completion: @escaping (Result<StatsBridgeDescriptor, Error>) -> Void) {
        queue.async { [weak self] in
            guard let self else { return }
            if self.listener != nil {
                if let descriptor = self.descriptor, descriptor.isUsable() {
                    completion(.success(descriptor))
                    return
                }
                do {
                    try self.rotateDescriptor()
                    guard let descriptor = self.descriptor, descriptor.isUsable() else {
                        completion(.failure(StatsBridgeServerError.descriptorWriteFailed))
                        return
                    }
                    completion(.success(descriptor))
                } catch {
                    completion(.failure(error))
                }
                return
            }
            self.pendingStartCompletions.append(completion)

            do {
                let parameters = NWParameters.tcp
                parameters.requiredLocalEndpoint = NWEndpoint.hostPort(host: .ipv4(.loopback), port: .any)
                let listener = try NWListener(using: parameters)
                self.listener = listener
                listener.stateUpdateHandler = { [weak self] state in
                    self?.handleListenerState(state)
                }
                listener.newConnectionHandler = { [weak self] connection in
                    self?.receiveRequest(on: connection, data: Data())
                }
                listener.start(queue: self.queue)
            } catch {
                self.finishStart(with: .failure(error))
            }
        }
    }

    /// Reports listener availability only; it never exposes the descriptor capability.
    func observeAvailability(_ observer: @escaping (Bool) -> Void) {
        queue.async { [weak self] in
            guard let self else { return }
            self.availabilityObserver = observer
            observer(self.listener != nil && self.descriptor?.isUsable() == true)
        }
    }

    func stop() {
        queue.async { [weak self] in
            guard let self else { return }
            self.rotationTimer?.cancel()
            self.rotationTimer = nil
            self.listener?.cancel()
            self.listener = nil
            self.descriptor = nil
            try? FileManager.default.removeItem(at: self.descriptorURL)
            self.availabilityObserver?(false)
            self.finishStart(with: .failure(StatsBridgeServerError.didNotStart))
        }
    }

    private func handleListenerState(_ state: NWListener.State) {
        switch state {
        case .ready:
            guard listener?.port != nil else {
                finishStart(with: .failure(StatsBridgeServerError.unavailablePort))
                return
            }
            do {
                try rotateDescriptor()
                scheduleDescriptorRotation()
                if let descriptor {
                    availabilityObserver?(true)
                    finishStart(with: .success(descriptor))
                } else {
                    finishStart(with: .failure(StatsBridgeServerError.descriptorWriteFailed))
                }
            } catch {
                listener?.cancel()
                listener = nil
                descriptor = nil
                try? FileManager.default.removeItem(at: descriptorURL)
                finishStart(with: .failure(error))
            }
        case .failed(let error):
            rotationTimer?.cancel()
            rotationTimer = nil
            listener?.cancel()
            listener = nil
            descriptor = nil
            try? FileManager.default.removeItem(at: descriptorURL)
            availabilityObserver?(false)
            finishStart(with: .failure(error))
        case .cancelled:
            rotationTimer?.cancel()
            rotationTimer = nil
            listener = nil
            descriptor = nil
            try? FileManager.default.removeItem(at: descriptorURL)
            availabilityObserver?(false)
        default:
            break
        }
    }

    private func scheduleDescriptorRotation() {
        rotationTimer?.cancel()
        let timer = DispatchSource.makeTimerSource(queue: queue)
        timer.schedule(deadline: .now() + Self.descriptorRefreshInterval, repeating: Self.descriptorRefreshInterval)
        timer.setEventHandler { [weak self] in
            try? self?.rotateDescriptor()
        }
        rotationTimer = timer
        timer.resume()
    }

    private func rotateDescriptor() throws {
        guard let port = listener?.port else { throw StatsBridgeServerError.unavailablePort }
        let updated = StatsBridgeDescriptor(
            schemaVersion: StatsBridgeDescriptor.schemaVersion,
            port: port.rawValue,
            capability: Self.makeCapability(),
            expiresAt: Date.now.addingTimeInterval(descriptorLifetime)
        )
        let parent = descriptorURL.deletingLastPathComponent()
        try FileManager.default.createDirectory(at: parent, withIntermediateDirectories: true)
        let data = try JSONEncoder().encode(updated)
        try data.write(to: descriptorURL, options: .atomic)
        try FileManager.default.setAttributes([.posixPermissions: 0o600], ofItemAtPath: descriptorURL.path)
        descriptor = updated
    }

    private func finishStart(with result: Result<StatsBridgeDescriptor, Error>) {
        let completions = pendingStartCompletions
        pendingStartCompletions.removeAll()
        completions.forEach { $0(result) }
    }

    private static func makeCapability() -> String {
        var bytes = [UInt8](repeating: 0, count: 32)
        guard SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes) == errSecSuccess else {
            return UUID().uuidString.replacingOccurrences(of: "-", with: "")
                + UUID().uuidString.replacingOccurrences(of: "-", with: "")
        }
        return Data(bytes).base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }

    private func receiveRequest(on connection: NWConnection, data: Data) {
        connection.start(queue: queue)
        connection.receive(minimumIncompleteLength: 1, maximumLength: Self.maximumRequestBytes) { [weak self] content, _, isComplete, error in
            guard let self else { return }
            if error != nil {
                connection.cancel()
                return
            }
            var combined = data
            if let content { combined.append(content) }
            if combined.count > Self.maximumRequestBytes {
                self.send(status: 413, body: Data(), on: connection)
                return
            }
            switch Self.parseRequest(combined) {
            case .incomplete where !isComplete:
                self.receiveRequest(on: connection, data: combined)
            case .incomplete, .invalid:
                self.send(status: 400, body: Data(), on: connection)
            case .complete(let request):
                self.handle(request, on: connection)
            }
        }
    }

    private func handle(_ request: HTTPRequest, on connection: NWConnection) {
        guard request.method == "POST", request.path == "/v1/roster" || request.path == "/v1/hypixel-key-validation" else {
            send(status: 404, body: Data(), on: connection)
            return
        }
        guard let descriptor, descriptor.isUsable(), request.headers["x-legitils-capability"] == descriptor.capability else {
            send(status: 401, body: Data(), on: connection)
            return
        }
        guard request.headers["content-type"]?.lowercased().hasPrefix("application/json") == true else {
            send(status: 400, body: Data(), on: connection)
            return
        }

        if request.path == "/v1/roster" {
            guard let roster = try? JSONDecoder().decode(StatsBridgeRosterRequest.self, from: request.body), roster.isValid else {
                send(status: 400, body: Data(), on: connection)
                return
            }
            lookup(roster) { [weak self] response in
                self?.queue.async {
                    guard let body = try? JSONEncoder().encode(response) else {
                        self?.send(status: 500, body: Data(), on: connection)
                        return
                    }
                    self?.send(status: 200, body: body, on: connection)
                }
            }
            return
        }

        guard let validation = try? JSONDecoder().decode(HypixelAPIKeyValidationRequest.self, from: request.body), validation.isValid else {
            send(status: 400, body: Data(), on: connection)
            return
        }
        hypixelKeyValidation { [weak self] status in
            self?.queue.async {
                let response = HypixelAPIKeyValidationResponse(
                    schemaVersion: HypixelAPIKeyValidationRequest.schemaVersion,
                    status: status
                )
                guard let body = try? JSONEncoder().encode(response) else {
                    self?.send(status: 500, body: Data(), on: connection)
                    return
                }
                self?.send(status: 200, body: body, on: connection)
            }
        }
    }

    private func send(status: Int, body: Data, on connection: NWConnection) {
        let reason: String
        switch status {
        case 200: reason = "OK"
        case 400: reason = "Bad Request"
        case 401: reason = "Unauthorized"
        case 404: reason = "Not Found"
        case 413: reason = "Payload Too Large"
        default: reason = "Internal Server Error"
        }
        var headers = "HTTP/1.1 \(status) \(reason)\r\nContent-Length: \(body.count)\r\nConnection: close\r\n"
        if !body.isEmpty { headers += "Content-Type: application/json\r\n" }
        headers += "\r\n"
        var output = Data(headers.utf8)
        output.append(body)
        connection.send(content: output, completion: .contentProcessed { _ in connection.cancel() })
    }
}

private extension StatsBridgeServer {
    struct HTTPRequest {
        let method: String
        let path: String
        let headers: [String: String]
        let body: Data
    }

    enum ParseResult {
        case incomplete
        case invalid
        case complete(HTTPRequest)
    }

    static func parseRequest(_ data: Data) -> ParseResult {
        let separator = Data("\r\n\r\n".utf8)
        guard let headerRange = data.range(of: separator) else { return .incomplete }
        guard let headerText = String(data: data[..<headerRange.lowerBound], encoding: .utf8) else { return .invalid }
        let lines = headerText.components(separatedBy: "\r\n")
        guard let requestLine = lines.first else { return .invalid }
        let parts = requestLine.split(separator: " ")
        guard parts.count == 3, parts[2] == "HTTP/1.1" else { return .invalid }
        var headers: [String: String] = [:]
        for line in lines.dropFirst() {
            guard let separator = line.firstIndex(of: ":") else { return .invalid }
            let name = String(line[..<separator]).lowercased()
            let value = line[line.index(after: separator)...].trimmingCharacters(in: .whitespaces)
            guard !name.isEmpty, headers[name] == nil else { return .invalid }
            headers[name] = value
        }
        guard let lengthText = headers["content-length"], let length = Int(lengthText), length >= 0, length <= maximumRequestBytes else {
            return .invalid
        }
        let bodyStart = headerRange.upperBound
        guard data.count >= bodyStart + length else { return .incomplete }
        guard data.count == bodyStart + length else { return .invalid }
        return .complete(HTTPRequest(
            method: String(parts[0]),
            path: String(parts[1]),
            headers: headers,
            body: Data(data[bodyStart..<(bodyStart + length)])
        ))
    }
}
