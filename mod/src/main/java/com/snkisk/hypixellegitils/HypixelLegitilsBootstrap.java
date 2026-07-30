package com.snkisk.hypixellegitils;

import com.snkisk.hypixellegitils.alert.AlertPresentation;
import com.snkisk.hypixellegitils.alert.ChatFormat;
import com.snkisk.hypixellegitils.alert.LocalAlertSink;
import com.snkisk.hypixellegitils.config.ConfigLoadResult;
import com.snkisk.hypixellegitils.config.ConfigPaths;
import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.config.DetectorSettingsService;
import com.snkisk.hypixellegitils.config.LegitilsConfig;
import com.snkisk.hypixellegitils.config.LegitilsConfigStore;
import com.snkisk.hypixellegitils.config.MarkerHistoryEntry;
import com.snkisk.hypixellegitils.config.MarkerHistoryStore;
import com.snkisk.hypixellegitils.config.RuntimeStatus;
import com.snkisk.hypixellegitils.command.LocalCommand;
import com.snkisk.hypixellegitils.profile.MojangProfileResolver;
import com.snkisk.hypixellegitils.observation.ObservationCoordinator;
import com.snkisk.hypixellegitils.detection.PlayerSample;
import com.snkisk.hypixellegitils.detection.BedNukeSignalCheck;
import com.snkisk.hypixellegitils.detection.NoBreakDelaySignalCheck;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Minimal startup/tick proof. Detection, networking, and gameplay behaviour belong to later phases. */
public final class HypixelLegitilsBootstrap {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final AtomicBoolean VISIBLE_PLAYER_OBSERVATION_STARTED = new AtomicBoolean(false);
    private static volatile ObservationCoordinator coordinator;
    private static volatile AlertPresentation currentPresentation;
    private static volatile DetectorSettingsService detectorSettings;
    private static volatile boolean nickDetectionEnabled = true;
    private static volatile boolean developerSelfDetectionEnabled;
    private static volatile UUID developmentSelfPlayerId;
    private static volatile boolean developmentFrameGlobalLag = true;
    private static final MojangProfileResolver PROFILE_RESOLVER = new MojangProfileResolver();
    private static final Queue<PendingBlacklistOperation> PENDING_BLACKLIST_OPERATIONS
        = new ConcurrentLinkedQueue<PendingBlacklistOperation>();
    private static final Queue<String> PENDING_NICK_NOTICES = new ConcurrentLinkedQueue<String>();
    private static final Set<UUID> NICKED_SESSION_PLAYER_IDS
        = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private static final AtomicBoolean NICK_OBSERVATION_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean MARKER_RENDER_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean TAB_RENDER_HOOK_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean NAME_RENDER_HOOK_LOGGED = new AtomicBoolean(false);
    private static final Set<String> PENDING_PROFILE_LOOKUP_KEYS
        = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final Object PROFILE_LOOKUP_LOCK = new Object();
    private static final int MAXIMUM_PENDING_PROFILE_LOOKUPS = 8;
    private static final AtomicInteger PENDING_PROFILE_LOOKUP_COUNT = new AtomicInteger(0);
    private static final AtomicLong BLACKLIST_GENERATION = new AtomicLong(0L);
    private static final ExecutorService PROFILE_LOOKUP_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "HypixelLegitils-MojangProfileLookup");
            thread.setDaemon(true);
            return thread;
        }
    });

    private HypixelLegitilsBootstrap() {
    }

    public static void onMinecraftStarted() {
        if (STARTED.compareAndSet(false, true)) {
            System.setProperty("hypixellegitils.bootstrap.started", "true");
            String userHome = System.getProperty("user.home", ".");
            LegitilsConfigStore store = new LegitilsConfigStore();
            Path configPath = ConfigPaths.configPath(userHome);
            ConfigLoadResult loaded = store.load(configPath);
            final MarkerHistoryStore markerHistoryStore = new MarkerHistoryStore();
            final Path markerHistoryPath = ConfigPaths.markerHistoryPath(userHome);
            coordinator = new ObservationCoordinator(
                loaded.config,
                new LocalAlertSink(loaded.config.notifications),
                markerHistoryStore.load(markerHistoryPath),
                new com.snkisk.hypixellegitils.observation.MarkerHistoryPersistence() {
                    @Override
                    public void write(Map<UUID, MarkerHistoryEntry> history) throws IOException {
                        markerHistoryStore.writeAtomically(markerHistoryPath, history);
                    }
                }
            );
            detectorSettings = new DetectorSettingsService(store, configPath, loaded.config);
            nickDetectionEnabled = loaded.config.nickDetectionSettings.enabled;
            developerSelfDetectionEnabled = loaded.config.debugEnabled;
            try {
                store.writeRuntimeStatusAtomically(
                    ConfigPaths.runtimeStatusPath(userHome),
                    new RuntimeStatus("0.1.0-SNAPSHOT", loaded.config.revision, loaded.usedDefaults)
                );
            } catch (Exception exception) {
                System.err.println("[HypixelLegitils] Runtime status unavailable: " + exception.getClass().getSimpleName());
            }
            System.out.println("[HypixelLegitils] Bootstrap Mixin reached Minecraft.startGame; " + loaded.diagnostic + ".");
        }
    }

    public static AlertPresentation onClientTick(long nowMillis) {
        ObservationCoordinator active = coordinator;
        if (!STARTED.get() || active == null) return null;
        AlertPresentation presentation = active.onClientTick(nowMillis);
        currentPresentation = presentation;
        return presentation;
    }

    public static void onObservedPlayers(List<PlayerSample> samples, boolean globalLag) {
        ObservationCoordinator active = coordinator;
        if (!STARTED.get() || active == null || samples == null) return;
        if (!samples.isEmpty() && VISIBLE_PLAYER_OBSERVATION_STARTED.compareAndSet(false, true)) {
            System.out.println("[HypixelLegitils] Visible-player observation active.");
        }
        active.beginObservationFrame(globalLag);
        active.observeFrame(samples);
    }

    /** Supplies the current client-tick lag state before development-only controller input can arrive. */
    public static void onDevelopmentFrame(boolean globalLag) {
        developmentFrameGlobalLag = globalLag;
        if (!globalLag) return;
        ObservationCoordinator active = coordinator;
        if (!STARTED.get() || active == null) return;
        active.onImmediateGlobalLag();
        UUID localPlayerId = developmentSelfPlayerId;
        if (developerSelfDetectionEnabled && localPlayerId != null) {
            active.observeDevelopmentTimerStall(localPlayerId, System.currentTimeMillis());
        }
    }

    /** Caches a non-nick server-presented name for an existing Blacklist entry; nick profiles stay session-only. */
    public static void onObservedPlayerIdentity(UUID playerId, String serverPresentedName) {
        if (playerId == null) return;
        if (playerId.version() != 1) {
            ObservationCoordinator active = coordinator;
            if (STARTED.get() && active != null) active.recordObservedServerName(playerId, serverPresentedName, System.currentTimeMillis());
            return;
        }
        if (!nickDetectionEnabled) return;
        if (NICKED_SESSION_PLAYER_IDS.size() >= 256 || !NICKED_SESSION_PLAYER_IDS.add(playerId)) return;
        if (serverPresentedName != null && !serverPresentedName.trim().isEmpty()) {
            PENDING_NICK_NOTICES.add(ChatFormat.line("§c" + serverPresentedName + "§5 is nicked."));
        }
        if (NICK_OBSERVATION_LOGGED.compareAndSet(false, true)) {
            System.out.println("[HypixelLegitils] Nick session marker observed for " + serverPresentedName + ".");
        }
    }

    /** A paused, skipped, or replaced client-world tick invalidates timing signals. */
    public static void onObservationDiscontinuity() {
        ObservationCoordinator active = coordinator;
        if (STARTED.get() && active != null) active.onObservationDiscontinuity();
    }

    public static AlertPresentation currentPresentation() {
        return currentPresentation;
    }

    /** Handles only the explicit local diagnostic namespace; null means pass through unchanged. */
    public static String[] localCommandResponses(String input, boolean addToChat) {
        return localCommandResponses(input, addToChat, Collections.<String, UUID>emptyMap());
    }

    /** Handles local commands; a missing local UUID is resolved asynchronously through Mojang after explicit user input. */
    public static String[] localCommandResponses(String input, boolean addToChat, Map<String, UUID> visiblePlayers) {
        ObservationCoordinator active = coordinator;
        LocalCommand.Request request = LocalCommand.requestForUserInput(input, addToChat);
        if (request == null) return null;
        if (active == null || detectorSettings == null) return new String[] { ChatFormat.line("§cStatus unavailable.") };
        if (request.kind == LocalCommand.Kind.STATUS) return overallStatusLines(detectorSettings.savedConfig(), active);
        if (request.kind == LocalCommand.Kind.HELP) return LocalCommand.helpLines();
        if (request.kind == LocalCommand.Kind.ANTICHEAT_LIST) return detectorListLines(active.statusText(), detectorSettings.savedConfig());
        if (request.kind == LocalCommand.Kind.ANTICHEAT_SET) return updateDetectorSetting(request);
        if (request.kind == LocalCommand.Kind.NICK_DETECT_SET_ENABLED) return updateNickDetectionSetting(request);
        if (request.kind == LocalCommand.Kind.DEV_SET_ENABLED) return updateDeveloperSetting(request);
        if (request.kind == LocalCommand.Kind.NOTIFICATION_SET_ENABLED) return updateNotificationSetting(request);
        if (request.kind == LocalCommand.Kind.MARKER_STATUS) {
            return blacklistStatusLines(detectorSettings.savedConfig(), active);
        }
        if (request.kind == LocalCommand.Kind.BLACKLIST_LIST) return blacklistListLines(active, request.threshold < 1 ? 1 : request.threshold);
        if (request.kind == LocalCommand.Kind.MARKER_SET_ENABLED || request.kind == LocalCommand.Kind.MARKER_SET_THRESHOLD) {
            return updateBlacklistSetting(request);
        }
        if (request.kind == LocalCommand.Kind.MARKER_CLEAR_ALL) return clearLocalBlacklist(active);
        if (request.kind == LocalCommand.Kind.BLACKLIST_ADD || request.kind == LocalCommand.Kind.BLACKLIST_REMOVE) {
            return updateManualBlacklist(request, active, visiblePlayers);
        }
        return LocalCommand.invalidLocalCommandLines();
    }

    /** Compatibility helper for callers that only need one local response line. */
    public static String localCommandResponse(String input, boolean addToChat) {
        String[] responses = localCommandResponses(input, addToChat);
        return responses == null || responses.length == 0 ? null : responses[0];
    }

    /** Called from the client thread to apply and display completed manual Mojang lookups. */
    public static String[] drainPendingBlacklistResponses() {
        List<String> responses = new ArrayList<String>();
        PendingBlacklistOperation operation;
        while ((operation = PENDING_BLACKLIST_OPERATIONS.poll()) != null) {
            releaseProfileLookup(operation.lookupKey);
            if (operation.generation != BLACKLIST_GENERATION.get()) continue;
            ObservationCoordinator active = coordinator;
            if (active == null) {
                responses.add(ChatFormat.line("§cBlacklist lookup finished after shutdown; no change was made."));
                continue;
            }
            if (operation.resolution.status == MojangProfileResolver.Status.FOUND) {
                String name = operation.resolution.canonicalName;
                boolean changed = operation.kind == LocalCommand.Kind.BLACKLIST_ADD
                    ? active.blacklistMarker(operation.resolution.playerId, System.currentTimeMillis())
                    : active.removeMarker(operation.resolution.playerId);
                if (changed && operation.kind == LocalCommand.Kind.BLACKLIST_ADD) {
                    active.recordMojangResolvedName(operation.resolution.playerId, name, System.currentTimeMillis());
                }
                if (changed) {
                    responses.add(ChatFormat.line(
                        (operation.kind == LocalCommand.Kind.BLACKLIST_ADD ? "§aAdded §f" : "§aRemoved §f")
                        + name + " §7" + (operation.kind == LocalCommand.Kind.BLACKLIST_ADD ? "to" : "from") + " the Blacklist."));
                } else {
                    responses.add(ChatFormat.line("§eBlacklist unchanged for §f" + name + "§7."));
                }
            } else if (operation.resolution.status == MojangProfileResolver.Status.NOT_FOUND) {
                responses.add(ChatFormat.line("§cNo Java profile was found for §f" + operation.requestedName + "§c."));
            } else {
                responses.add(ChatFormat.line("§cMojang profile lookup is unavailable. Try again later."));
            }
        }
        return responses.toArray(new String[responses.size()]);
    }

    /** Called on the client thread; each session Nick produces one local chat notification. */
    public static String[] drainPendingNickNotices() {
        List<String> notices = new ArrayList<String>();
        String notice;
        while ((notice = PENDING_NICK_NOTICES.poll()) != null) notices.add(notice);
        return notices.toArray(new String[notices.size()]);
    }

    private static String[] updateDetectorSetting(LocalCommand.Request request) {
        try {
            DetectorSettingsService.Update update = request.all
                ? detectorSettings.setAllEnabled(request.enabled)
                : detectorSettings.setEnabled(request.detector, request.enabled);
            String target = request.all ? "all detectors" : request.detector.displayName();
            String state = request.enabled ? "enabled" : "disabled";
            ObservationCoordinator active = coordinator;
            if (active == null) throw new IllegalStateException("Observation coordinator unavailable");
            active.applyRuntimeDetectorConfig(update.config);
            String prefix = update.changed ? "saved and applied" : "already active";
            return new String[] {
                ChatFormat.line("§a" + prefix + " §e" + target + " §f" + state),
                ChatFormat.continuation("§7Active: §a" + enabledCount(update.config) + "§8/§a" + availableCount() + " §7detectors")
            };
        } catch (DetectorSettingsService.ConfigWriteRefusedException exception) {
            return new String[] { ChatFormat.line("§cConfiguration changed or invalid. Detectors unchanged.") };
        } catch (Exception exception) {
            return new String[] { ChatFormat.line("§cUnable to save detector setting. Detectors unchanged.") };
        }
    }

    private static String[] detectorListLines(String activeStatus, LegitilsConfig activeConfig) {
        return new String[] {
            ChatFormat.line("§fAnti-cheat settings"),
            ChatFormat.continuation("§7Current: §f" + activeStatus),
            ChatFormat.continuation("§7Enabled: §a" + enabledCount(activeConfig) + "§8/§a" + availableCount()
                + " §7enabled §8[§f" + enabledNames(activeConfig) + "§8]"),
            ChatFormat.continuation("§7Use §b.l anticheat on/off <detector|all> §7to toggle immediately.")
        };
    }

    private static String[] updateBlacklistSetting(LocalCommand.Request request) {
        try {
            DetectorSettingsService.Update update = request.kind == LocalCommand.Kind.MARKER_SET_ENABLED
                ? detectorSettings.setMarkerEnabled(request.enabled)
                : detectorSettings.setMarkerThreshold(request.threshold);
            ObservationCoordinator active = coordinator;
            if (active == null) throw new IllegalStateException("Observation coordinator unavailable");
            active.applyRuntimeMarkerConfig(update.config);
            String detail = request.kind == LocalCommand.Kind.MARKER_SET_ENABLED
                ? (request.enabled ? "enabled" : "disabled")
                : "threshold " + update.config.markerSettings.threshold;
            return new String[] {
                ChatFormat.line("§eAuto blacklist §f" + detail + (update.changed ? " §asaved and applied" : " §7already active")),
                ChatFormat.continuation("§7Threshold: §e" + update.config.markerSettings.threshold + " §7accepted flags")
            };
        } catch (IllegalArgumentException exception) {
            return new String[] { ChatFormat.line("§cBlacklist threshold must be 2-10.") };
        } catch (DetectorSettingsService.ConfigWriteRefusedException exception) {
            return new String[] { ChatFormat.line("§cConfiguration changed or invalid. Blacklist unchanged.") };
        } catch (Exception exception) {
            return new String[] { ChatFormat.line("§cUnable to save Blacklist setting. Blacklist unchanged.") };
        }
    }

    private static String[] updateNickDetectionSetting(LocalCommand.Request request) {
        try {
            DetectorSettingsService.Update update = detectorSettings.setNickDetectionEnabled(request.enabled);
            ObservationCoordinator active = coordinator;
            if (active == null) throw new IllegalStateException("Observation coordinator unavailable");
            active.applyRuntimeNickDetectionConfig(update.config);
            nickDetectionEnabled = update.config.nickDetectionSettings.enabled;
            if (!nickDetectionEnabled) {
                NICKED_SESSION_PLAYER_IDS.clear();
            }
            return new String[] {
                ChatFormat.line("§fNick detect " + (nickDetectionEnabled ? "§aenabled" : "§cdisabled")
                    + (update.changed ? " §asaved and applied" : " §7already active")),
                ChatFormat.continuation("§7Changes apply immediately.")
            };
        } catch (DetectorSettingsService.ConfigWriteRefusedException exception) {
            return new String[] { ChatFormat.line("§cConfiguration changed or invalid. Nick detect unchanged.") };
        } catch (Exception exception) {
            return new String[] { ChatFormat.line("§cUnable to save Nick detect setting. Nick detect unchanged.") };
        }
    }

    private static String[] updateNotificationSetting(LocalCommand.Request request) {
        try {
            DetectorSettingsService.Update update = detectorSettings.setNotificationEnabled(request.notificationChannel, request.enabled);
            ObservationCoordinator active = coordinator;
            if (active == null) throw new IllegalStateException("Observation coordinator unavailable");
            active.applyRuntimeNotificationConfig(update.config);
            return new String[] {
                ChatFormat.line("§f" + request.notificationChannel.displayName() + " notifications "
                    + (request.enabled ? "§aenabled" : "§cdisabled")
                    + (update.changed ? " §asaved and applied" : " §7already active"))
            };
        } catch (DetectorSettingsService.ConfigWriteRefusedException exception) {
            return new String[] { ChatFormat.line("§cConfiguration changed or invalid. Notifications unchanged.") };
        } catch (Exception exception) {
            return new String[] { ChatFormat.line("§cUnable to save notification setting. Notifications unchanged.") };
        }
    }

    private static String[] updateDeveloperSetting(LocalCommand.Request request) {
        try {
            DetectorSettingsService.Update update = detectorSettings.setDebugEnabled(request.enabled);
            ObservationCoordinator active = coordinator;
            if (active == null) throw new IllegalStateException("Observation coordinator unavailable");
            active.applyRuntimeDevelopmentConfig(update.config);
            developerSelfDetectionEnabled = update.config.debugEnabled;
            if (!developerSelfDetectionEnabled) developmentSelfPlayerId = null;
            return new String[] {
                ChatFormat.line("§fDeveloper self-detect " + (developerSelfDetectionEnabled ? "§aenabled" : "§cdisabled")
                    + (update.changed ? " §asaved and applied" : " §7already active")),
                ChatFormat.continuation("§7Only affects whether your own player is observed locally.")
            };
        } catch (DetectorSettingsService.ConfigWriteRefusedException exception) {
            return new String[] { ChatFormat.line("§cConfiguration changed or invalid. Developer mode unchanged.") };
        } catch (Exception exception) {
            return new String[] { ChatFormat.line("§cUnable to save developer mode. Developer mode unchanged.") };
        }
    }

    private static String[] overallStatusLines(LegitilsConfig config, ObservationCoordinator active) {
        String nickState = config.nickDetectionSettings.enabled ? "§aenabled" : "§cdisabled";
        return new String[] {
            ChatFormat.line("§fStatus"),
            ChatFormat.continuation("§7Anti-cheat: §a" + enabledCount(config) + "§8/§a" + availableCount() + " §7detectors active"),
            ChatFormat.continuation("§7Nick detect: " + nickState),
            ChatFormat.continuation("§7Developer self-detect: " + (config.debugEnabled ? "§aenabled" : "§cdisabled")),
            ChatFormat.continuation("§7Auto blacklist: " + (config.markerSettings.enabled ? "§aenabled" : "§cdisabled")
                + " §8| §7threshold: §e" + config.markerSettings.threshold + " §7accepted flags"),
            ChatFormat.continuation("§7Blacklist: §e" + active.blacklistedMarkerCount() + " §7active §8/ §e" + active.markerHistoryCount()
                + " §7stored UUIDs"),
            ChatFormat.continuation("§7Notifications: §fChat " + (config.notifications.chatEnabled ? "§aon" : "§coff")
                + " §8| §7Action Bar " + (config.notifications.overlayEnabled ? "§aon" : "§coff")
                + " §8| §7Sound " + (config.notifications.soundEnabled ? "§aon" : "§coff"))
        };
    }

    private static String[] clearLocalBlacklist(ObservationCoordinator active) {
        BLACKLIST_GENERATION.incrementAndGet();
        synchronized (PROFILE_LOOKUP_LOCK) {
            PENDING_PROFILE_LOOKUP_KEYS.clear();
        }
        if (active.clearAllMarkers()) {
            return new String[] { ChatFormat.line("§aBlacklist history cleared.") };
        }
        return new String[] { ChatFormat.line("§eBlacklist history is already empty.") };
    }

    private static String[] updateManualBlacklist(
        LocalCommand.Request request,
        ObservationCoordinator active,
        Map<String, UUID> visiblePlayers
    ) {
        UUID playerId = findVisiblePlayerId(request.playerName, visiblePlayers);
        if (playerId == null) {
            return resolveManualBlacklistThroughMojang(request);
        }
        if (playerId.version() == 1) {
            return new String[] { ChatFormat.line("§cNick profiles cannot be added to the Blacklist.") };
        }
        return applyManualBlacklistUpdate(request, active, playerId, request.playerName);
    }

    private static String[] applyManualBlacklistUpdate(
        LocalCommand.Request request,
        ObservationCoordinator active,
        UUID playerId,
        String playerName
    ) {
        boolean changed = request.kind == LocalCommand.Kind.BLACKLIST_ADD
            ? active.blacklistMarker(playerId, System.currentTimeMillis())
            : active.removeMarker(playerId);
        if (changed && request.kind == LocalCommand.Kind.BLACKLIST_ADD) {
            active.recordObservedServerName(playerId, playerName, System.currentTimeMillis());
        }
        if (!changed) {
            return new String[] { ChatFormat.line("§eBlacklist unchanged for §f" + playerName + "§7.") };
        }
        return new String[] {
            ChatFormat.line((request.kind == LocalCommand.Kind.BLACKLIST_ADD ? "§aAdded §f" : "§aRemoved §f")
                + playerName + " §7" + (request.kind == LocalCommand.Kind.BLACKLIST_ADD ? "to" : "from") + " the Blacklist.")
        };
    }

    private static String[] resolveManualBlacklistThroughMojang(final LocalCommand.Request request) {
        final long generation = BLACKLIST_GENERATION.get();
        final String lookupKey = generation + ":" + request.kind.name() + ":" + request.playerName.toLowerCase(Locale.ROOT);
        synchronized (PROFILE_LOOKUP_LOCK) {
            if (PENDING_PROFILE_LOOKUP_KEYS.contains(lookupKey)) {
                return new String[] { ChatFormat.line("§eAlready resolving §f" + request.playerName + "§7.") };
            }
            if (PENDING_PROFILE_LOOKUP_COUNT.get() >= MAXIMUM_PENDING_PROFILE_LOOKUPS) {
                return new String[] { ChatFormat.line("§cToo many profile lookups are pending. Try again shortly.") };
            }
            PENDING_PROFILE_LOOKUP_KEYS.add(lookupKey);
            PENDING_PROFILE_LOOKUP_COUNT.incrementAndGet();
        }
        try {
            PROFILE_LOOKUP_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    PENDING_BLACKLIST_OPERATIONS.add(new PendingBlacklistOperation(
                        request.kind,
                        request.playerName,
                        PROFILE_RESOLVER.resolve(request.playerName),
                        generation,
                        lookupKey
                    ));
                }
            });
            return new String[] { ChatFormat.line("§7Resolving §f" + request.playerName + " §7through Mojang...") };
        } catch (Exception exception) {
            releaseProfileLookup(lookupKey);
            return new String[] { ChatFormat.line("§cMojang profile lookup is unavailable. Try again later.") };
        }
    }

    private static void releaseProfileLookup(String lookupKey) {
        synchronized (PROFILE_LOOKUP_LOCK) {
            PENDING_PROFILE_LOOKUP_KEYS.remove(lookupKey);
            if (PENDING_PROFILE_LOOKUP_COUNT.get() > 0) PENDING_PROFILE_LOOKUP_COUNT.decrementAndGet();
        }
    }

    private static UUID findVisiblePlayerId(String playerName, Map<String, UUID> visiblePlayers) {
        if (playerName == null || visiblePlayers == null) return null;
        return visiblePlayers.get(playerName.toLowerCase(Locale.ROOT));
    }

    private static final class PendingBlacklistOperation {
        private final LocalCommand.Kind kind;
        private final String requestedName;
        private final MojangProfileResolver.Resolution resolution;
        private final long generation;
        private final String lookupKey;

        private PendingBlacklistOperation(
            LocalCommand.Kind kind,
            String requestedName,
            MojangProfileResolver.Resolution resolution,
            long generation,
            String lookupKey
        ) {
            this.kind = kind;
            this.requestedName = requestedName;
            this.resolution = resolution;
            this.generation = generation;
            this.lookupKey = lookupKey;
        }
    }

    private static String[] blacklistStatusLines(LegitilsConfig config, ObservationCoordinator active) {
        return new String[] {
            ChatFormat.line("§fBlacklist"),
            ChatFormat.continuation("§7Auto blacklist: " + (config.markerSettings.enabled ? "§aenabled" : "§cdisabled")
                + " §8| §7threshold: §e" + config.markerSettings.threshold + " §7accepted flags"),
            ChatFormat.continuation("§7Stored UUIDs: §e" + active.markerHistoryCount() + " §8| §7blacklisted: §e" + active.blacklistedMarkerCount())
        };
    }

    private static String[] blacklistListLines(ObservationCoordinator active, int requestedPage) {
        List<Map.Entry<UUID, MarkerHistoryEntry>> entries = new ArrayList<Map.Entry<UUID, MarkerHistoryEntry>>();
        for (Map.Entry<UUID, MarkerHistoryEntry> entry : active.markerHistory().entrySet()) {
            if (entry.getValue().blacklisted) entries.add(entry);
        }
        Collections.sort(entries, new java.util.Comparator<Map.Entry<UUID, MarkerHistoryEntry>>() {
            @Override
            public int compare(Map.Entry<UUID, MarkerHistoryEntry> left, Map.Entry<UUID, MarkerHistoryEntry> right) {
                return Long.compare(right.getValue().updatedAtEpochMillis, left.getValue().updatedAtEpochMillis);
            }
        });
        if (entries.isEmpty()) return new String[] { ChatFormat.line("§eBlacklist is empty.") };
        final int pageSize = 5;
        int pages = (entries.size() + pageSize - 1) / pageSize;
        int page = Math.min(requestedPage, pages);
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, entries.size());
        List<String> lines = new ArrayList<String>();
        lines.add(ChatFormat.line("§fBlacklist §7page §e" + page + "§7/§e" + pages));
        for (int index = start; index < end; index++) {
            Map.Entry<UUID, MarkerHistoryEntry> entry = entries.get(index);
            MarkerHistoryEntry value = entry.getValue();
            boolean preferObserved = value.observedServerName != null
                && value.observedServerNameAtEpochMillis >= value.mojangResolvedAtEpochMillis;
            String name = preferObserved ? value.observedServerName : value.mojangResolvedName;
            String source = preferObserved ? "server seen" : "Mojang cached";
            if (name == null) {
                name = "name unavailable";
                source = "UUID only";
            }
            lines.add(ChatFormat.continuation("§e⚠ §f" + name + " §8[§7" + source + "§8]"));
            lines.add(ChatFormat.continuation("§8" + entry.getKey().toString()));
        }
        if (pages > 1) lines.add(ChatFormat.continuation("§7Use §e.l blacklist list <page> §7for more entries."));
        return lines.toArray(new String[lines.size()]);
    }

    private static int enabledCount(LegitilsConfig config) {
        int enabled = 0;
        for (DetectorId detector : DetectorId.values()) if (detector.isImplementedInCurrentBuild() && config.isDetectorEnabled(detector)) enabled++;
        return enabled;
    }

    private static int availableCount() {
        int available = 0;
        for (DetectorId detector : DetectorId.values()) if (detector.isImplementedInCurrentBuild()) available++;
        return available;
    }

    private static String enabledNames(LegitilsConfig config) {
        StringBuilder names = new StringBuilder();
        for (DetectorId detector : DetectorId.values()) {
            if (!detector.isImplementedInCurrentBuild() || !config.isDetectorEnabled(detector)) continue;
            if (names.length() > 0) names.append(", ");
            names.append(detector.displayName());
        }
        return names.length() == 0 ? "none" : names.toString();
    }

    public static void onBedStructure(BedNukeSignalCheck.BedStructure structure, long nowMillis) {
        ObservationCoordinator active = coordinator;
        if (STARTED.get() && active != null) active.observeBedStructure(structure, nowMillis);
    }

    public static void onBedBlockState(BedNukeSignalCheck.BlockPosition position, BedNukeSignalCheck.BlockKind state, long nowMillis) {
        ObservationCoordinator active = coordinator;
        if (STARTED.get() && active != null) active.observeBedBlockState(position, state, nowMillis);
    }

    public static void onNoBreakDelayProgress(NoBreakDelaySignalCheck.Progress progress) {
        ObservationCoordinator active = coordinator;
        if (STARTED.get() && active != null) active.observeNoBreakDelayProgress(progress);
    }

    public static void onNoBreakDelayBlockRemoval(NoBreakDelaySignalCheck.BlockPosition position, long worldTick, boolean completeContext) {
        ObservationCoordinator active = coordinator;
        if (STARTED.get() && active != null) active.observeNoBreakDelayBlockRemoval(position, worldTick, completeContext);
    }

    /** Read-only local controller input used only while `.l dev on` is active. */
    public static void onDevelopmentNoBreakDelay(UUID playerId, long worldTick, int blockHitDelay, boolean breakCompleted) {
        ObservationCoordinator active = coordinator;
        if (!STARTED.get() || !developerSelfDetectionEnabled || developmentFrameGlobalLag || active == null || playerId == null) return;
        active.setDevelopmentSelfPlayerId(playerId);
        active.observeDevelopmentNoBreakDelay(playerId, worldTick, blockHitDelay, breakCompleted);
    }

    /** Read-only rendering hook for a currently visible entity or Tab profile. */
    public static boolean shouldShowAcceptedAlertMarker(java.util.UUID playerId) {
        ObservationCoordinator active = coordinator;
        return STARTED.get() && active != null && active.shouldShowAcceptedAlertMarker(playerId, System.currentTimeMillis());
    }

    /** Session-only local nick marker; it is deliberately separate from the persistent Blacklist. */
    public static boolean shouldShowNickedSessionMarker(java.util.UUID playerId) {
        return nickDetectionEnabled && playerId != null && NICKED_SESSION_PLAYER_IDS.contains(playerId);
    }

    /** Tab profiles can arrive before a matching world entity; UUID-v1 remains a session-only Nick marker. */
    public static boolean shouldShowNickedProfileMarker(java.util.UUID playerId) {
        return nickDetectionEnabled && playerId != null && playerId.version() == 1;
    }

    /** Logs the first successful local marker render without adding a user-facing notification. */
    public static void onMarkerRenderObserved(java.util.UUID playerId, String marker) {
        if (playerId == null || marker == null || !MARKER_RENDER_LOGGED.compareAndSet(false, true)) return;
        System.out.println("[HypixelLegitils] Marker render hook observed: " + marker + ".");
    }

    public static void onMarkerRenderHookObserved(String hook) {
        AtomicBoolean logged = "tab".equals(hook) ? TAB_RENDER_HOOK_LOGGED : NAME_RENDER_HOOK_LOGGED;
        if (logged.compareAndSet(false, true)) System.out.println("[HypixelLegitils] " + hook + " marker render hook active.");
    }

    /** Development-only adapter switch: production observations exclude the local player. */
    public static boolean shouldObserveLocalPlayerForDevelopment() {
        return STARTED.get() && developerSelfDetectionEnabled;
    }

    /** Identifies the local development sample so accepted flags cannot auto-Blacklist the local account. */
    public static void onDeveloperSelfPlayerObserved(UUID playerId) {
        ObservationCoordinator active = coordinator;
        if (STARTED.get() && developerSelfDetectionEnabled && active != null && playerId != null) {
            developmentSelfPlayerId = playerId;
            active.setDevelopmentSelfPlayerId(playerId);
        }
    }

    public static void onChunkTransition() {
        ObservationCoordinator active = coordinator;
        if (STARTED.get() && active != null) active.onChunkTransition();
    }

    public static void onWorldLoading() {
        ObservationCoordinator active = coordinator;
        developmentFrameGlobalLag = true;
        developmentSelfPlayerId = null;
        if (active != null) {
            active.onWorldLoading();
            currentPresentation = null;
            VISIBLE_PLAYER_OBSERVATION_STARTED.set(false);
            NICKED_SESSION_PLAYER_IDS.clear();
            PENDING_NICK_NOTICES.clear();
            NICK_OBSERVATION_LOGGED.set(false);
            MARKER_RENDER_LOGGED.set(false);
            TAB_RENDER_HOOK_LOGGED.set(false);
            NAME_RENDER_HOOK_LOGGED.set(false);
            System.out.println("[HypixelLegitils] World lifecycle reset.");
        }
    }

    public static boolean isStarted() {
        return STARTED.get();
    }
}
