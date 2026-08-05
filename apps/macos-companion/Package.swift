// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "HypixelLegitilsCompanion",
    platforms: [.macOS(.v13)],
    products: [
        .executable(name: "HypixelLegitilsCompanion", targets: ["HypixelLegitilsCompanion"])
    ],
    targets: [
        .executableTarget(
            name: "HypixelLegitilsCompanion",
            path: "Sources/HypixelLegitilsCompanion"
        ),
        .testTarget(
            name: "HypixelLegitilsCompanionTests",
            dependencies: ["HypixelLegitilsCompanion"],
            path: "Tests/HypixelLegitilsCompanionTests"
        )
    ]
)
