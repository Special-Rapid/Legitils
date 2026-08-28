import Darwin

if BackgroundRuntimePreparer.isRequested(arguments: CommandLine.arguments) {
    exit(BackgroundRuntimePreparer().run())
}

HypixelLegitilsCompanionApp.main()
