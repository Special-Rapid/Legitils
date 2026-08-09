package com.snkisk.hypixellegitils;

import com.snkisk.hypixellegitils.alert.AlertPresentation;
import com.snkisk.hypixellegitils.alert.ChatFormat;
import com.snkisk.hypixellegitils.alert.FlagMessage;
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
import com.snkisk.hypixellegitils.config.StatsSettings;
import com.snkisk.hypixellegitils.command.LocalCommand;
import com.snkisk.hypixellegitils.profile.MojangProfileResolver;
import com.snkisk.hypixellegitils.observation.ObservationCoordinator;
import com.snkisk.hypixellegitils.detection.PlayerSample;
import com.snkisk.hypixellegitils.detection.BedNukeSignalCheck;
import com.snkisk.hypixellegitils.detection.NoBreakDelaySignalCheck;
import com.snkisk.hypixellegitils.nick.NickChatSignal;
import com.snkisk.hypixellegitils.nick.BedDestructionChatSignal;
import com.snkisk.hypixellegitils.party.BedwarsPreGameState;
import com.snkisk.hypixellegitils.party.PartyScoreboardJumpDetector;
import com.snkisk.hypixellegitils.stats.StatsBridgeClient;
import com.snkisk.hypixellegitils.stats.BedwarsMode;
import com.snkisk.hypixellegitils.stats.StatsBridgeLookupResult;
import com.snkisk.hypixellegitils.stats.StatsBridgeRosterMember;
import com.snkisk.hypixellegitils.stats.StatsBridgeSession;
import com.snkisk.hypixellegitils.stats.StatsMatchRequestGate;
import com.snkisk.hypixellegitils.stats.StatsPresentation;
import com.snkisk.hypixellegitils.stats.StatsBridgePlayerResult;
import com.snkisk.hypixellegitils.stats.StatsTabSorter;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import com.snkisk.hypixellegitils.stats.WhoStatsRefresh;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
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
    private static volatile boolean partyDetectionEnabled = true;
    private static volatile StatsSettings statsSettings = StatsSettings.defaults();
    private static volatile boolean developerSelfDetectionEnabled;
    private static volatile UUID developmentSelfPlayerId;
    private static volatile boolean developmentFrameGlobalLag = true;
    private static final long EXTERNAL_CONFIG_POLL_INTERVAL_MILLIS = 500L;
    private static volatile long nextExternalConfigPollMillis;
    private static volatile String runtimeStatusUserHome;
    private static volatile StatsBridgeClient statsBridgeClient;
    private static volatile StatsBridgeLookupResult latestStatsBridgeResult = StatsBridgeLookupResult.unavailable();
    private static volatile boolean statsTraceEnabled;
    private static final StatsMatchRequestGate STATS_MATCH_REQUEST_GATE = new StatsMatchRequestGate();
    private static final StatsBridgeSession STATS_BRIDGE_SESSION = new StatsBridgeSession();
    private static final Object STATS_BRIDGE_RESULT_LOCK = new Object();
    private static final MojangProfileResolver PROFILE_RESOLVER = new MojangProfileResolver();
    private static final Queue<PendingBlacklistOperation> PENDING_BLACKLIST_OPERATIONS
        = new ConcurrentLinkedQueue<PendingBlacklistOperation>();
    private static final Queue<String> PENDING_NICK_NOTICES = new ConcurrentLinkedQueue<String>();
    private static final Queue<PendingTeamNickNotice> PENDING_TEAM_NICK_NOTICES
        = new ConcurrentLinkedQueue<PendingTeamNickNotice>();
    private static final Map<UUID, String> PREGAME_NICK_CHATTERS = new ConcurrentHashMap<UUID, String>();
    private static final Set<String> PREGAME_STATS_CHATTERS
        = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final int MAXIMUM_PREGAME_STATS_LOOKUP_ATTEMPTS = 2;
    private static final Map<String, AtomicInteger> PREGAME_STATS_LOOKUP_ATTEMPTS
        = new ConcurrentHashMap<String, AtomicInteger>();
    private static final AtomicLong PREGAME_STATS_LOOKUP_SEQUENCE = new AtomicLong(0L);
    private static final WhoStatsRefresh.PendingRequests PENDING_WHO_STATS_REFRESHES
        = new WhoStatsRefresh.PendingRequests(8);
    private static final Queue<String> PENDING_PARTY_DETECTOR_NOTICES = new ConcurrentLinkedQueue<String>();
    private static final Queue<PendingStatsNotice> PENDING_STATS_NOTICES = new ConcurrentLinkedQueue<PendingStatsNotice>();
    private static final Queue<String> PENDING_CONFIGURATION_NOTICES = new ConcurrentLinkedQueue<String>();
    private static final Queue<StatsBridgeLookupResult> PENDING_MANUAL_STATS_RESULTS
        = new ConcurrentLinkedQueue<StatsBridgeLookupResult>();
    private static final AtomicLong MANUAL_STATS_LOOKUP_SEQUENCE = new AtomicLong(0L);
    private static final PartyScoreboardJumpDetector PARTY_SCOREBOARD_JUMPS = new PartyScoreboardJumpDetector();
    private static final Set<UUID> NICKED_SESSION_PLAYER_IDS
        = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private static final AtomicBoolean NICK_OBSERVATION_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean MARKER_RENDER_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean TAB_RENDER_HOOK_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean NAME_RENDER_HOOK_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean NAME_RENDER_SUFFIX_DECISION_LOGGED = new AtomicBoolean(false);
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
    private static final ExecutorService STATS_BRIDGE_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "HypixelLegitils-StatsBridge");
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
            runtimeStatusUserHome = userHome;
            statsBridgeClient = new StatsBridgeClient(ConfigPaths.statsBridgeDescriptorPath(userHome));
            nickDetectionEnabled = loaded.config.nickDetectionSettings.enabled;
            partyDetectionEnabled = loaded.config.partyDetectionSettings.enabled;
            statsSettings = loaded.config.statsSettings;
            developerSelfDetectionEnabled = loaded.config.debugEnabled;
            try {
                store.writeRuntimeStatusAtomically(
                    ConfigPaths.runtimeStatusPath(userHome),
                    new RuntimeStatus(BuildInfo.displayVersion(), loaded.config.revision, loaded.usedDefaults)
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
        reloadExternalConfigIfNeeded(nowMillis, active);
        AlertPresentation presentation = active.onClientTick(nowMillis);
        currentPresentation = presentation;
        return presentation;
    }

    private static void reloadExternalConfigIfNeeded(long nowMillis, ObservationCoordinator active) {
        if (nowMillis < nextExternalConfigPollMillis) return;
        nextExternalConfigPollMillis = nowMillis + EXTERNAL_CONFIG_POLL_INTERVAL_MILLIS;
        DetectorSettingsService settings = detectorSettings;
        if (settings == null) return;
        DetectorSettingsService.ExternalReload reload = settings.reloadFromDiskIfNewer();
        if (!reload.applied || reload.config == null) return;
        active.applyRuntimeExternalConfig(reload.config);
        nickDetectionEnabled = reload.config.nickDetectionSettings.enabled;
        partyDetectionEnabled = reload.config.partyDetectionSettings.enabled;
        statsSettings = reload.config.statsSettings;
        developerSelfDetectionEnabled = reload.config.debugEnabled;
        enqueueCompanionSettingsApplied(reload.config.revision);
        if (!nickDetectionEnabled) {
            NICKED_SESSION_PLAYER_IDS.clear();
            PREGAME_NICK_CHATTERS.clear();
        }
        if (!partyDetectionEnabled) resetPartyDetectors();
        try {
            String userHome = runtimeStatusUserHome == null ? System.getProperty("user.home", ".") : runtimeStatusUserHome;
            new LegitilsConfigStore().writeRuntimeStatusAtomically(
                ConfigPaths.runtimeStatusPath(userHome),
                new RuntimeStatus(BuildInfo.displayVersion(), reload.config.revision, false)
            );
        } catch (Exception exception) {
            System.err.println("[HypixelLegitils] Runtime status unavailable: " + exception.getClass().getSimpleName());
        }
    }

    /** Receives only the actor name parsed from a server Bed-destruction announcement. */
    public static void onBedDestructionChat(String rawMessage, long nowMillis) {
        String serverName = BedDestructionChatSignal.destroyedBy(rawMessage);
        ObservationCoordinator active = coordinator;
        if (STARTED.get() && active != null && serverName != null) {
            active.observeBedDestructionChat(serverName, nowMillis);
        }
    }

    /** Observes visible Bed Wars pre-game player-count changes only. */
    public static void onPartyDetectorTick(BedwarsPreGameState.PlayerCount playerCount) {
        if (!STARTED.get()) {
            resetPartyDetectors();
            return;
        }
        if (!partyDetectionEnabled) {
            resetPartyDetectors();
            return;
        }
        enqueuePartyDetectorNotice(PARTY_SCOREBOARD_JUMPS.observe(playerCount));
    }

    public static String[] drainPendingPartyDetectorNotices() {
        List<String> notices = new ArrayList<String>();
        String notice;
        while ((notice = PENDING_PARTY_DETECTOR_NOTICES.poll()) != null) notices.add(notice);
        return notices.toArray(new String[notices.size()]);
    }

    private static void enqueuePartyDetectorNotice(int playerCountChange) {
        if (playerCountChange >= 2) {
            PENDING_PARTY_DETECTOR_NOTICES.add(
                ChatFormat.line("§fParty of §c" + playerCountChange + " §fjoined.")
            );
        } else if (playerCountChange <= -2) {
            PENDING_PARTY_DETECTOR_NOTICES.add(
                ChatFormat.line("§fParty of §c" + (-playerCountChange) + " §fquit.")
            );
        }
    }

    private static void resetPartyDetectors() {
        PARTY_SCOREBOARD_JUMPS.reset();
        PENDING_PARTY_DETECTOR_NOTICES.clear();
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
    public static void onObservedPlayerIdentity(
        UUID playerId,
        String serverPresentedName,
        String formattedDisplayName,
        boolean bedwarsPreGame
    ) {
        if (playerId == null) return;
        if (playerId.version() != 1) {
            ObservationCoordinator active = coordinator;
            if (STARTED.get() && active != null) active.recordObservedServerName(playerId, serverPresentedName, System.currentTimeMillis());
            return;
        }
        if (!nickDetectionEnabled) return;
        // During pre-game, only a Nick who actually chats is announced at game
        // start. The Tab marker remains available without creating chat noise.
        if (bedwarsPreGame) return;
        if (NICKED_SESSION_PLAYER_IDS.size() >= 256 || !NICKED_SESSION_PLAYER_IDS.add(playerId)) return;
        if (serverPresentedName != null && !serverPresentedName.trim().isEmpty()) {
            if (!FlagMessage.hasBedWarsTeamPrefix(formattedDisplayName)) {
                PENDING_TEAM_NICK_NOTICES.add(new PendingTeamNickNotice(playerId, serverPresentedName, System.currentTimeMillis() + 1500L));
            } else {
                PENDING_NICK_NOTICES.add(ChatFormat.line(
                    FlagMessage.teamFormattedName(formattedDisplayName, serverPresentedName) + "§5 is nicked."
                ));
            }
        }
        if (NICK_OBSERVATION_LOGGED.compareAndSet(false, true)) {
            System.out.println("[HypixelLegitils] Nick session marker observed for " + serverPresentedName + ".");
        }
    }

    /** Queues a pre-game Nick until Bed Wars assigns their team at game start. */
    public static void onPregameNickChat(UUID playerId, String serverPresentedName) {
        if (!STARTED.get() || !nickDetectionEnabled || playerId == null || playerId.version() != 1
            || serverPresentedName == null || serverPresentedName.trim().isEmpty()) return;
        if (NICKED_SESSION_PLAYER_IDS.size() >= 256 || !NICKED_SESSION_PLAYER_IDS.add(playerId)) return;
        PREGAME_NICK_CHATTERS.put(playerId, serverPresentedName);
    }

    /** Releases one pre-game Nick notice once Hypixel announces game start and the team is available. */
    public static void onPregameGameStartChat(String rawMessage, long nowMillis) {
        if (!NickChatSignal.isGameStart(rawMessage)) return;
        for (Map.Entry<UUID, String> entry : PREGAME_NICK_CHATTERS.entrySet()) {
            PENDING_TEAM_NICK_NOTICES.add(new PendingTeamNickNotice(entry.getKey(), entry.getValue(), nowMillis + 1000L));
        }
        PREGAME_NICK_CHATTERS.clear();
    }

    /** Called only after a visible Bed Wars pre-game start message. */
    public static void onBedwarsGameStart(long nowMillis) {
        if (!STARTED.get()) return;
        STATS_MATCH_REQUEST_GATE.onBedwarsGameStart(nowMillis);
        traceStats("roster awaits game-world transition from start countdown");
    }

    /** A visibly cancelled start is not a completed match; the next countdown may schedule once. */
    public static void onBedwarsGameStartCancelled() {
        STATS_MATCH_REQUEST_GATE.reset();
        traceStats("roster reset from pregame cancellation");
    }

    /** Schedules the settled roster only when Lunar visibly transitions from Bed Wars pre-game to game. */
    public static void onBedwarsPregameState(boolean active, long nowMillis) {
        if (STATS_MATCH_REQUEST_GATE.onPregameState(active, nowMillis)) {
            traceStats("pregame end scheduled post-start roster");
        }
    }

    /** Returns one opaque match ID after the short roster-settle delay. */
    public static String consumeDueStatsMatchId(long nowMillis) {
        return STARTED.get() ? STATS_MATCH_REQUEST_GATE.consumeDueMatchId(nowMillis) : null;
    }

    /** Checks the roster-settle delay without consuming it, so an unparsed sidebar can retry. */
    public static boolean isStatsRosterDue(long nowMillis) {
        return STARTED.get() && STATS_MATCH_REQUEST_GATE.isDue(nowMillis);
    }

    /** Runs outside the client thread; it never contacts remote providers or exposes their keys. */
    public static void requestStatsRoster(
        final String matchId,
        final BedwarsMode gameMode,
        final List<StatsBridgeRosterMember> players
    ) {
        requestStatsRoster(matchId, gameMode, players, Collections.<String, String>emptyMap());
    }

    /** Observes an ordinary user-entered `/who` without changing or consuming the outgoing command. */
    public static boolean onWhoCommandSubmitted(String message) {
        WhoStatsRefresh.Submission submission = WhoStatsRefresh.submissionFor(message);
        if (!submission.shouldRefresh) return false;
        StatsSettings settings = statsSettings;
        if (!STARTED.get() || !settings.enabled || statsBridgeClient == null) return false;
        long generation = STATS_BRIDGE_SESSION.currentGeneration();
        if (PENDING_WHO_STATS_REFRESHES.enqueue(submission, generation) == null) {
            traceStats("who refresh skipped queue limit");
            return false;
        }
        traceStats("who refresh queued");
        return true;
    }

    /** The client-thread roster collector consumes each explicit or automatic `/who` refresh once. */
    public static String consumePendingWhoStatsRefresh() {
        return STARTED.get() ? PENDING_WHO_STATS_REFRESHES.consume() : null;
    }

    /** Post-start uses this opaque ID for the one automatic `/who` refresh, replacing the old parallel roster request. */
    public static String automaticWhoStatsRefreshMatchId(String postStartMatchId) {
        if (postStartMatchId == null || postStartMatchId.trim().isEmpty()
            || !STARTED.get() || !statsSettings.enabled || statsBridgeClient == null) return null;
        return PENDING_WHO_STATS_REFRESHES.nextAutomaticMatchId(STATS_BRIDGE_SESSION.currentGeneration());
    }

    /** Carries the local visible team formatting only until the normalized Bridge result is presented. */
    public static void requestStatsRoster(
        final String matchId,
        final BedwarsMode gameMode,
        final List<StatsBridgeRosterMember> players,
        Map<String, String> teamFormattedNames
    ) {
        final StatsBridgeClient client = statsBridgeClient;
        if (client == null || matchId == null || players == null || players.isEmpty()) {
            traceStats("roster request skipped client=" + (client != null) + " mode=" + gameMode
                + " players=" + (players == null ? -1 : players.size()));
            return;
        }
        traceStats("roster request queued mode=" + gameMode + " players=" + players.size());
        final long sessionGeneration = STATS_BRIDGE_SESSION.currentGeneration();
        final Map<String, String> visibleTeamNames = Collections.unmodifiableMap(
            new java.util.LinkedHashMap<String, String>(teamFormattedNames == null
                ? Collections.<String, String>emptyMap() : teamFormattedNames)
        );
        STATS_BRIDGE_EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                if (!STATS_BRIDGE_SESSION.isCurrent(sessionGeneration)) return;
                StatsBridgeLookupResult result = client.requestOnce(matchId, gameMode, players, System.currentTimeMillis());
                traceStats("roster bridge result=" + result.status + " players=" + result.players.size());
                publishStatsBridgeResult(sessionGeneration, result, visibleTeamNames);
            }
        });
    }

    /**
     * Looks up one visible pre-game chatter through the local Companion. The Companion first
     * verifies that the presented name belongs to a public Mojang profile; a failed lookup is
     * not treated as a Nick and no identity mapping is returned to this MOD.
     */
    public static void onPregameStatsChat(String serverPresentedName, BedwarsMode gameMode) {
        StatsSettings settings = statsSettings;
        if (!STARTED.get() || !settings.enabled || serverPresentedName == null) {
            traceStats("pregame chat skipped started=" + STARTED.get() + " stats=" + settings.enabled
                + " chat=" + settings.chatEnabled + " mode=" + gameMode);
            return;
        }
        StatsBridgeRosterMember chatter = new StatsBridgeRosterMember(serverPresentedName, null);
        if (!chatter.isValid()) {
            traceStats("pregame chat skipped invalid visible sender");
            return;
        }
        final StatsBridgeClient client = statsBridgeClient;
        if (client == null) {
            traceStats("pregame chat skipped bridge client unavailable");
            return;
        }
        String key = serverPresentedName.toLowerCase(Locale.ROOT);
        if (PREGAME_STATS_CHATTERS.size() >= 64 || !PREGAME_STATS_CHATTERS.add(key)) {
            traceStats("pregame chat skipped duplicate or limit");
            return;
        }
        AtomicInteger attempts = PREGAME_STATS_LOOKUP_ATTEMPTS.get(key);
        if (attempts == null) {
            AtomicInteger created = new AtomicInteger(0);
            AtomicInteger existing = PREGAME_STATS_LOOKUP_ATTEMPTS.putIfAbsent(key, created);
            attempts = existing == null ? created : existing;
        }
        if (attempts.incrementAndGet() > MAXIMUM_PREGAME_STATS_LOOKUP_ATTEMPTS) {
            PREGAME_STATS_CHATTERS.remove(key);
            traceStats("pregame chat skipped retry limit");
            return;
        }
        traceStats("pregame chat request queued mode=" + gameMode);
        final long sessionGeneration = STATS_BRIDGE_SESSION.currentGeneration();
        final String requestId = "pregame_" + sessionGeneration + "_" + PREGAME_STATS_LOOKUP_SEQUENCE.incrementAndGet();
        final List<StatsBridgeRosterMember> players = Collections.singletonList(chatter);
        STATS_BRIDGE_EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                if (!STATS_BRIDGE_SESSION.isCurrent(sessionGeneration)) return;
                StatsBridgeLookupResult result = client.requestOnce(requestId, gameMode, players, System.currentTimeMillis());
                traceStats("pregame chat bridge result=" + result.status + " players=" + result.players.size());
                if (result.status == StatsBridgeLookupResult.Status.UNAVAILABLE) PREGAME_STATS_CHATTERS.remove(key);
                publishPregameStatsResult(sessionGeneration, result);
            }
        });
    }

    private static void publishStatsBridgeResult(
        long sessionGeneration,
        StatsBridgeLookupResult result,
        Map<String, String> teamFormattedNames
    ) {
        synchronized (STATS_BRIDGE_RESULT_LOCK) {
            if (!STATS_BRIDGE_SESSION.isCurrent(sessionGeneration)) return;
            latestStatsBridgeResult = result;
            traceStats("roster result published chat=" + statsSettings.chatEnabled + " tab=" + statsSettings.tabEnabled);
            if (statsSettings.enabled && statsSettings.chatEnabled) {
                for (StatsPresentation.ChatNotice notice : StatsPresentation.chatNotices(result, teamFormattedNames)) {
                    PENDING_STATS_NOTICES.add(new PendingStatsNotice(ChatFormat.line(notice.text), notice.tooltip, notice.tagCode));
                }
            }
        }
    }

    /** Pregame chatter results merge only resolved chatters for display until the later complete match roster supersedes them. */
    private static void publishPregameStatsResult(long sessionGeneration, StatsBridgeLookupResult result) {
        synchronized (STATS_BRIDGE_RESULT_LOCK) {
            if (!STATS_BRIDGE_SESSION.isCurrent(sessionGeneration) || !statsSettings.enabled) return;
            if (result.status == StatsBridgeLookupResult.Status.READY) {
                latestStatsBridgeResult = mergePregameStatsResult(latestStatsBridgeResult, result);
            }
            traceStats("pregame chat result published");
            if (statsSettings.chatEnabled) {
                for (StatsPresentation.ChatNotice notice : StatsPresentation.pregameChatNotices(result)) {
                    PENDING_STATS_NOTICES.add(new PendingStatsNotice(ChatFormat.line(notice.text), notice.tooltip, notice.tagCode));
                }
            }
        }
    }

    /** Retains each resolved pregame chatter for Tab/Nametag until the complete after-start roster supersedes it. */
    private static StatsBridgeLookupResult mergePregameStatsResult(StatsBridgeLookupResult current, StatsBridgeLookupResult incoming) {
        Map<String, StatsBridgePlayerResult> players = new LinkedHashMap<String, StatsBridgePlayerResult>();
        if (current != null && current.status == StatsBridgeLookupResult.Status.READY) {
            for (StatsBridgePlayerResult player : current.players) players.put(player.name.toLowerCase(Locale.ROOT), player);
        }
        for (StatsBridgePlayerResult player : incoming.players) players.put(player.name.toLowerCase(Locale.ROOT), player);
        return StatsBridgeLookupResult.ready(new ArrayList<StatsBridgePlayerResult>(players.values()));
    }

    /** Retained only as normalized Bridge data for the forthcoming local display stage. */
    public static StatsBridgeLookupResult latestStatsBridgeResult() {
        return latestStatsBridgeResult;
    }

    /** Opt-in local trace for automatic Stats stages; it never includes player names, keys, or API payloads. */
    public static void traceStats(String stage) {
        if (statsTraceEnabled && stage != null) System.out.println("[HypixelLegitils][StatsTrace] " + stage);
    }

    /** Returns a local-only suffix for a known real profile; existing Tab markers stay ahead of it. */
    public static String statsTabSuffix(String playerName, UUID playerId) {
        StatsSettings settings = statsSettings;
        if (!STARTED.get() || !settings.enabled || !settings.tabEnabled || playerName == null || playerId == null || playerId.version() == 1) return "";
        StatsBridgeLookupResult result = latestStatsBridgeResult;
        if (result.status != StatsBridgeLookupResult.Status.READY) return "";
        for (StatsBridgePlayerResult player : result.players) {
            if (playerName.equalsIgnoreCase(player.name)) return StatsPresentation.tabSuffix(player, settings);
        }
        return "";
    }

    /** Reorders only the local Tab render snapshot; server roster, teams, and packets remain untouched. */
    public static List<NetworkPlayerInfo> sortedTabPlayers(List<NetworkPlayerInfo> vanillaOrder) {
        StatsSettings settings = statsSettings;
        StatsBridgeLookupResult result = latestStatsBridgeResult;
        if (vanillaOrder == null || !STARTED.get() || !settings.enabled
            || (!settings.tabTeamSortingEnabled && !settings.tabPlayerSortingEnabled)
            || result.status != StatsBridgeLookupResult.Status.READY) return vanillaOrder;
        Map<String, StatsBridgePlayerResult> byName = new LinkedHashMap<String, StatsBridgePlayerResult>();
        for (StatsBridgePlayerResult player : result.players) {
            if (player != null && player.name != null) byName.put(player.name.toLowerCase(Locale.ROOT), player);
        }
        List<StatsTabSorter.Entry<NetworkPlayerInfo>> entries = new ArrayList<StatsTabSorter.Entry<NetworkPlayerInfo>>(vanillaOrder.size());
        for (int index = 0; index < vanillaOrder.size(); index++) {
            NetworkPlayerInfo info = vanillaOrder.get(index);
            String playerName = info == null || info.getGameProfile() == null ? null : info.getGameProfile().getName();
            StatsBridgePlayerResult player = playerName == null ? null : byName.get(playerName.toLowerCase(Locale.ROOT));
            ScorePlayerTeam team = info == null ? null : info.getPlayerTeam();
            String teamKey = team == null ? null : team.getRegisteredName();
            UUID playerId = info == null || info.getGameProfile() == null ? null : info.getGameProfile().getId();
            boolean nicked = shouldShowNickedSessionMarker(playerId)
                || player != null && player.nickStatus == StatsBridgePlayerResult.NickStatus.NICKED;
            Double fkdr = !nicked && player != null && player.nickStatus == StatsBridgePlayerResult.NickStatus.KNOWN
                ? player.finalKillDeathRatio : null;
            entries.add(new StatsTabSorter.Entry<NetworkPlayerInfo>(info, teamKey, nicked, fkdr, index));
        }
        return StatsTabSorter.sort(entries, settings);
    }

    /** Returns the opt-in FKDR suffix for the vanilla player-nametag render path. */
    public static String statsNametagSuffix(String playerName, UUID playerId) {
        StatsSettings settings = statsSettings;
        if (!STARTED.get() || !settings.enabled || !settings.nametagEnabled || playerName == null || playerId == null || playerId.version() == 1) return "";
        StatsBridgeLookupResult result = latestStatsBridgeResult;
        if (result.status != StatsBridgeLookupResult.Status.READY) return "";
        for (StatsBridgePlayerResult player : result.players) {
            if (playerName.equalsIgnoreCase(player.name)) return StatsPresentation.nametagFkdrSuffix(player, settings);
        }
        return "";
    }

    /** Provider tag codes are local display data and intentionally do not inherit the optional FKDR toggle. */
    public static String statsNametagTagSuffix(String playerName, UUID playerId) {
        StatsSettings settings = statsSettings;
        if (!STARTED.get() || !settings.enabled || playerName == null || playerId == null || playerId.version() == 1) return "";
        StatsBridgeLookupResult result = latestStatsBridgeResult;
        if (result.status != StatsBridgeLookupResult.Status.READY) return "";
        for (StatsBridgePlayerResult player : result.players) {
            if (playerName.equalsIgnoreCase(player.name)) return StatsPresentation.nametagTagSuffix(player);
        }
        return "";
    }

    /** Returns all local-only markers for the renderer's actual player-nametag text. */
    public static String playerNametagSuffix(String playerName, UUID playerId) {
        String suffix = "";
        if (shouldShowNickedSessionMarker(playerId)) suffix += " §c[NICK]";
        if (shouldShowAcceptedAlertMarker(playerId)) suffix += " §e⚠";
        return suffix + statsNametagTagSuffix(playerName, playerId) + statsNametagSuffix(playerName, playerId);
    }

    /**
     * Extends Lunar's Adventure name component with a legacy-formatted local
     * suffix. Reflection keeps the ordinary Forge build independent from
     * Lunar's private runtime library while preserving every existing style on
     * the server-provided component.
     */
    public static Object appendLunarNametagComponentSuffix(Object original, String playerName, UUID playerId) {
        if (original == null) return null;
        String suffix = playerNametagSuffix(playerName, playerId);
        if (suffix.isEmpty()) {
            traceNameTagSuffixDecision(true, false);
            return original;
        }
        traceNameTagSuffixDecision(true, true);
        onMarkerRenderObserved(playerId, suffix);
        try {
            ClassLoader loader = original.getClass().getClassLoader();
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component", false, loader);
            Class<?> serializerClass = Class.forName(
                "net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer",
                false,
                loader
            );
            Object serializer = serializerClass.getMethod("legacySection").invoke(null);
            Object suffixComponent = serializerClass.getMethod("deserialize", String.class).invoke(serializer, suffix);
            @SuppressWarnings("unchecked")
            List<Object> children = new ArrayList<Object>((List<Object>) componentClass.getMethod("children").invoke(original));
            children.add(suffixComponent);
            return componentClass.getMethod("children", List.class).invoke(original, children);
        } catch (ReflectiveOperationException e) {
            traceStats("name-tag Adventure component unavailable");
            return original;
        }
    }

    public static PendingStatsNotice[] drainPendingStatsNotices() {
        List<PendingStatsNotice> notices = new ArrayList<PendingStatsNotice>();
        PendingStatsNotice notice;
        while ((notice = PENDING_STATS_NOTICES.poll()) != null) notices.add(notice);
        return notices.toArray(new PendingStatsNotice[notices.size()]);
    }

    /** Emits settings changes that arrived from the Companion rather than a local Minecraft command. */
    public static String[] drainPendingConfigurationNotices() {
        List<String> notices = new ArrayList<String>();
        String notice;
        while ((notice = PENDING_CONFIGURATION_NOTICES.poll()) != null) notices.add(notice);
        return notices.toArray(new String[notices.size()]);
    }

    static void enqueueCompanionSettingsApplied(long revision) {
        PENDING_CONFIGURATION_NOTICES.add(ChatFormat.line("§aCompanion settings applied. §7Revision §f" + revision));
    }

    /** Emits completed explicit API-test results on the client thread. */
    public static PendingStatsNotice[] drainPendingManualStatsNotices() {
        List<PendingStatsNotice> responses = new ArrayList<PendingStatsNotice>();
        StatsBridgeLookupResult result;
        while ((result = PENDING_MANUAL_STATS_RESULTS.poll()) != null) {
            for (StatsPresentation.ChatNotice notice : StatsPresentation.manualLookupNotices(result)) {
                responses.add(new PendingStatsNotice(ChatFormat.line(notice.text), notice.tooltip, notice.tagCode));
            }
        }
        return responses.toArray(new PendingStatsNotice[responses.size()]);
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
        if (request.kind == LocalCommand.Kind.STATS_STATUS) return statsStatusLines(detectorSettings.savedConfig().statsSettings);
        if (request.kind == LocalCommand.Kind.STATS_SET) return updateStatsSetting(request);
        if (request.kind == LocalCommand.Kind.STATS_TRACE_SET_ENABLED) return updateStatsTrace(request.enabled);
        if (request.kind == LocalCommand.Kind.STATS_LOOKUP) {
            requestManualStatsLookup(request.playerName, visiblePlayers);
            return new String[] { ChatFormat.line("§bStats lookup started for §f" + request.playerName + "§b.") };
        }
        if (request.kind == LocalCommand.Kind.ANTICHEAT_LIST) return detectorListLines(active.statusText(), detectorSettings.savedConfig());
        if (request.kind == LocalCommand.Kind.ANTICHEAT_SET) return updateDetectorSetting(request);
        if (request.kind == LocalCommand.Kind.NICK_DETECT_SET_ENABLED) return updateNickDetectionSetting(request);
        if (request.kind == LocalCommand.Kind.PARTY_DETECT_SET_ENABLED) return updatePartyDetectionSetting(request);
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

    /** Explicit user lookup; it works even when automatic Stats presentation is disabled. */
    private static void requestManualStatsLookup(String playerName, Map<String, UUID> visiblePlayers) {
        if (playerName == null) return;
        StatsBridgeRosterMember requested = new StatsBridgeRosterMember(playerName, null);
        if (!requested.isValid()) return;
        UUID visibleId = visiblePlayers == null ? null : visiblePlayers.get(playerName.toLowerCase(Locale.ROOT));
        if (visibleId != null && visibleId.version() != 1) requested = new StatsBridgeRosterMember(playerName, visibleId.toString());
        final StatsBridgeClient client = statsBridgeClient;
        if (client == null) {
            PENDING_MANUAL_STATS_RESULTS.add(StatsBridgeLookupResult.unavailable());
            return;
        }
        final long sessionGeneration = STATS_BRIDGE_SESSION.currentGeneration();
        final String requestId = "manual_" + sessionGeneration + "_" + MANUAL_STATS_LOOKUP_SEQUENCE.incrementAndGet();
        final List<StatsBridgeRosterMember> players = Collections.singletonList(requested);
        STATS_BRIDGE_EXECUTOR.submit(new Runnable() {
            @Override
            public void run() {
                if (!STATS_BRIDGE_SESSION.isCurrent(sessionGeneration)) return;
                StatsBridgeLookupResult result = client.requestOnce(requestId, BedwarsMode.FOURS, players, System.currentTimeMillis());
                if (STATS_BRIDGE_SESSION.isCurrent(sessionGeneration)) PENDING_MANUAL_STATS_RESULTS.add(result);
            }
        });
    }

    private static String[] updateStatsSetting(LocalCommand.Request request) {
        try {
            StatsSettings current = detectorSettings.savedConfig().statsSettings;
            StatsSettings replacement = statsSettingsWith(current, request.statsOption, request.enabled);
            if (request.statsOption == LocalCommand.StatsOption.NAMETAG && request.enabled) {
                replacement = statsSettingsWith(current, request.statsOption, true, request.statsThreshold);
            }
            DetectorSettingsService.Update update = detectorSettings.setStatsSettings(replacement);
            statsSettings = update.config.statsSettings;
            return new String[] {
                ChatFormat.line("§fStats " + request.statsOption.displayName() + " "
                    + (request.enabled ? "§aenabled" : "§cdisabled")
                    + (request.statsOption == LocalCommand.StatsOption.NAMETAG && request.enabled
                        ? " §7at FKDR §f" + decimal(request.statsThreshold) + "+" : "")
                    + (update.changed ? " §asaved and applied" : " §7already active")),
                ChatFormat.continuation("§7Changes apply immediately.")
            };
        } catch (DetectorSettingsService.ConfigWriteRefusedException exception) {
            return new String[] { ChatFormat.line("§cConfiguration changed or invalid. Stats unchanged.") };
        } catch (Exception exception) {
            return new String[] { ChatFormat.line("§cUnable to save Stats setting. Stats unchanged.") };
        }
    }

    private static String[] updateStatsTrace(boolean enabled) {
        statsTraceEnabled = enabled;
        return new String[] { ChatFormat.line(enabled
            ? "§dStats trace enabled. §7Check latest.log and run §b.l log off §7when finished."
            : "§dStats trace disabled.") };
    }

    private static StatsSettings statsSettingsWith(StatsSettings current, LocalCommand.StatsOption option, boolean enabled) {
        return statsSettingsWith(current, option, enabled, current == null ? Double.NaN : current.nametagFkdrThreshold);
    }

    private static StatsSettings statsSettingsWith(StatsSettings current, LocalCommand.StatsOption option, boolean enabled, double nametagFkdrThreshold) {
        if (current == null || option == null) throw new IllegalArgumentException("Stats option is required");
        boolean stats = current.enabled;
        boolean tab = current.tabEnabled;
        boolean chat = current.chatEnabled;
        boolean stars = current.starsEnabled;
        boolean fkdr = current.fkdrEnabled;
        boolean winStreak = current.winStreakEnabled;
        boolean nametag = current.nametagEnabled;
        double nametagThreshold = current.nametagFkdrThreshold;
        boolean teamSort = current.tabTeamSortingEnabled;
        boolean playerSort = current.tabPlayerSortingEnabled;
        if (option == LocalCommand.StatsOption.ENABLED) stats = enabled;
        else if (option == LocalCommand.StatsOption.TAB) tab = enabled;
        else if (option == LocalCommand.StatsOption.CHAT) chat = enabled;
        else if (option == LocalCommand.StatsOption.STARS) stars = enabled;
        else if (option == LocalCommand.StatsOption.FKDR) fkdr = enabled;
        else if (option == LocalCommand.StatsOption.WIN_STREAK) winStreak = enabled;
        else if (option == LocalCommand.StatsOption.NAMETAG) {
            nametag = enabled;
            if (enabled) nametagThreshold = nametagFkdrThreshold;
        } else if (option == LocalCommand.StatsOption.TAB_TEAM_SORT) teamSort = enabled;
        else if (option == LocalCommand.StatsOption.TAB_PLAYER_SORT) playerSort = enabled;
        return new StatsSettings(stats, tab, stars, fkdr, winStreak, chat, nametag, nametagThreshold, teamSort, playerSort);
    }

    private static String[] statsStatusLines(StatsSettings settings) {
        if (settings == null) return new String[] { ChatFormat.line("§cStats status unavailable.") };
        return new String[] {
            ChatFormat.line("§fStats: " + state(settings.enabled)),
            ChatFormat.continuation("§7Tab: " + state(settings.tabEnabled) + " §8| §7Chat: " + state(settings.chatEnabled)),
            ChatFormat.continuation("§7Stars: " + state(settings.starsEnabled) + " §8| §7FKDR: " + state(settings.fkdrEnabled)
                + " §8| §7Win Streak: " + state(settings.winStreakEnabled)),
            ChatFormat.continuation("§7Nametag FKDR: " + state(settings.nametagEnabled)
                + " §8| §7threshold: §f" + decimal(settings.nametagFkdrThreshold)),
            ChatFormat.continuation("§7Tab sort: §fTeams " + state(settings.tabTeamSortingEnabled)
                + " §8| §fPlayers " + state(settings.tabPlayerSortingEnabled)
                + " §8| §7Nick = FKDR 5.0 for team score"),
            ChatFormat.continuation("§7Provider tags: §e[BC] [CC] [CF] [S] [PS] [LS] [A] [B] [AN] [CA] §7in Chat, Tab, and Nametag; Chat hover shows the API explanation."),
            ChatFormat.continuation("§7BC Blatant §8| §7CC Closet §8| §7CF Confirmed §8| §7S Sniper §8| §7PS Possible §8| §7LS Legit Sniper"),
            ChatFormat.continuation("§7A Account/Alt §8| §7B Bot §8| §7AN Annoying §8| §7CA Caution"),
            ChatFormat.continuation("§7/who: §fserver command + fresh local Stats refresh §8| §7post-start: §fone automatic /who"),
            ChatFormat.continuation("§7API keys stay in the Companion Keychain. §b.l stats <player> §7tests providers.")
        };
    }

    private static String state(boolean enabled) {
        return enabled ? "§aon" : "§coff";
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
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

    public static PendingTeamNickNotice[] drainPendingTeamNickNotices(long nowMillis) {
        List<PendingTeamNickNotice> notices = new ArrayList<PendingTeamNickNotice>();
        while (true) {
            PendingTeamNickNotice notice = PENDING_TEAM_NICK_NOTICES.peek();
            if (notice == null || nowMillis < notice.displayAfterMillis) break;
            notice = PENDING_TEAM_NICK_NOTICES.poll();
            if (notice != null) notices.add(notice);
        }
        return notices.toArray(new PendingTeamNickNotice[notices.size()]);
    }

    public static String pregameNickNotice(String serverPresentedName, String formattedDisplayName) {
        return ChatFormat.line(FlagMessage.teamFormattedName(formattedDisplayName, serverPresentedName) + "§5 is nicked.");
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

    private static String[] updatePartyDetectionSetting(LocalCommand.Request request) {
        try {
            DetectorSettingsService.Update update = detectorSettings.setPartyDetectionEnabled(request.enabled);
            partyDetectionEnabled = update.config.partyDetectionSettings.enabled;
            if (!partyDetectionEnabled) {
                resetPartyDetectors();
            }
            return new String[] {
                ChatFormat.line("§fParty detect " + (partyDetectionEnabled ? "§aenabled" : "§cdisabled")
                    + (update.changed ? " §asaved and applied" : " §7already active")),
                ChatFormat.continuation("§7Changes apply immediately.")
            };
        } catch (DetectorSettingsService.ConfigWriteRefusedException exception) {
            return new String[] { ChatFormat.line("§cConfiguration changed or invalid. Party detect unchanged.") };
        } catch (Exception exception) {
            return new String[] { ChatFormat.line("§cUnable to save Party detect setting. Party detect unchanged.") };
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
            if (!developerSelfDetectionEnabled) {
                developmentSelfPlayerId = null;
                resetPartyDetectors();
            }
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
        String partyState = config.partyDetectionSettings.enabled ? "§aenabled" : "§cdisabled";
        List<String> lines = new ArrayList<String>();
        lines.add(ChatFormat.line("§fStatus §8| §7Build §f" + BuildInfo.displayVersion()));
        lines.add(ChatFormat.continuation("§7Anti-cheat: §a" + enabledCount(config) + "§8/§a" + availableCount() + " §7detectors active"));
        lines.add(ChatFormat.continuation("§7Nick detect: " + nickState));
        lines.add(ChatFormat.continuation("§7Party detect: " + partyState));
        lines.add(ChatFormat.continuation("§7Developer self-detect: " + (config.debugEnabled ? "§aenabled" : "§cdisabled")));
        lines.add(ChatFormat.continuation("§7Auto blacklist: " + (config.markerSettings.enabled ? "§aenabled" : "§cdisabled")
            + " §8| §7threshold: §e" + config.markerSettings.threshold + " §7accepted flags"));
        lines.add(ChatFormat.continuation("§7Blacklist: §e" + active.blacklistedMarkerCount() + " §7active §8/ §e" + active.markerHistoryCount()
            + " §7stored UUIDs"));
        lines.add(ChatFormat.continuation("§7Notifications: §fChat " + (config.notifications.chatEnabled ? "§aon" : "§coff")
            + " §8| §7Action Bar " + (config.notifications.overlayEnabled ? "§aon" : "§coff")
            + " §8| §7Sound " + (config.notifications.soundEnabled ? "§aon" : "§coff")));
        lines.add(ChatFormat.continuation("§7Stats: " + state(config.statsSettings.enabled)
            + " §8| §7Tab " + state(config.statsSettings.tabEnabled)
            + " §8| §7Chat " + state(config.statsSettings.chatEnabled)));
        lines.add(ChatFormat.continuation("§7Stats fields: §fStars " + state(config.statsSettings.starsEnabled)
            + " §8| §fFKDR " + state(config.statsSettings.fkdrEnabled)
            + " §8| §fWin Streak " + state(config.statsSettings.winStreakEnabled)));
        lines.add(ChatFormat.continuation("§7Nametag FKDR: " + state(config.statsSettings.nametagEnabled)
            + " §8| §7threshold: §f" + decimal(config.statsSettings.nametagFkdrThreshold)
            + " §8| §7Nick [NICK]: " + nickState + " §8| §7Alert ⚠: " + state(config.markerSettings.enabled)));
        lines.add(ChatFormat.continuation("§7Tab sort: §fTeams " + state(config.statsSettings.tabTeamSortingEnabled)
            + " §8| §fPlayers " + state(config.statsSettings.tabPlayerSortingEnabled)
            + " §8| §7Team score = FKDR + Nick 5.0"));
        lines.add(ChatFormat.continuation("§7Provider tags: §e[BC] [CC] [CF] [S] [PS] [LS] [A] [B] [AN] [CA] §7Chat/Tab/Nametag §8| §7Chat hover: §aexplanation"));
        lines.add(ChatFormat.continuation("§7Codes: BC Blatant §8| §7CC Closet §8| §7CF Confirmed §8| §7S Sniper §8| §7PS Possible §8| §7LS Legit Sniper"));
        lines.add(ChatFormat.continuation("§7A Account/Alt §8| §7B Bot §8| §7AN Annoying §8| §7CA Caution"));
        lines.add(ChatFormat.continuation("§7Stats refresh: §f/who sends normally and refreshes once §8| §7post-start auto /who: §aone request"));
        lines.add(ChatFormat.continuation("§7Stats Bridge: " + statsBridgeState()
            + " §8| §7Trace: " + state(statsTraceEnabled)));
        lines.add(ChatFormat.continuation("§7Providers: §fHypixel/Urchin §7Companion Keychain §8| §fSeraph §apublic"));
        return lines.toArray(new String[lines.size()]);
    }

    private static String statsBridgeState() {
        StatsBridgeLookupResult.Status status = latestStatsBridgeResult.status;
        if (status == StatsBridgeLookupResult.Status.READY) return "§aready";
        if (status == StatsBridgeLookupResult.Status.ALREADY_REQUESTED) return "§epending";
        return "§cunavailable";
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

    /** Local-only Chat payload; tooltip text is already bounded and sanitised by the Companion and bridge parser. */
    public static final class PendingStatsNotice {
        public final String text;
        public final String tooltip;
        public final String tagCode;

        private PendingStatsNotice(String text, String tooltip, String tagCode) {
            this.text = text;
            this.tooltip = tooltip;
            this.tagCode = tagCode;
        }
    }

    /** Small client-thread payload; the Nick alias is never persisted beyond the current world. */
    public static final class PendingTeamNickNotice {
        public final UUID playerId;
        public final String serverPresentedName;
        private final long displayAfterMillis;

        private PendingTeamNickNotice(UUID playerId, String serverPresentedName, long displayAfterMillis) {
            this.playerId = playerId;
            this.serverPresentedName = serverPresentedName;
            this.displayAfterMillis = displayAfterMillis;
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

    /** Receives a remote player animation aimed at a Bed, never an inferred nearby-player identity. */
    public static void onBedBreakAttempt(UUID playerId, String serverName, boolean obstructed, long nowMillis) {
        ObservationCoordinator active = coordinator;
        if (STARTED.get() && active != null) active.observeBedBreakAttempt(playerId, serverName, obstructed, nowMillis);
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
        return nickDetectionEnabled && playerId != null && (playerId.version() == 1 || NICKED_SESSION_PLAYER_IDS.contains(playerId));
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

    /** Bounded opt-in trace for P0: no name, UUID, key, or provider payload is logged. */
    public static void traceNameTagSuffixDecision(boolean profileAvailable, boolean suffixAvailable) {
        if (!statsTraceEnabled || !NAME_RENDER_SUFFIX_DECISION_LOGGED.compareAndSet(false, true)) return;
        System.out.println("[HypixelLegitils][StatsTrace] name-tag candidate profile=" + profileAvailable
            + " suffix=" + suffixAvailable);
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
        boolean postStartRosterScheduled = STATS_MATCH_REQUEST_GATE.onWorldLoading(System.currentTimeMillis());
        developmentFrameGlobalLag = true;
        developmentSelfPlayerId = null;
        resetPartyDetectors();
        synchronized (STATS_BRIDGE_RESULT_LOCK) {
            STATS_BRIDGE_SESSION.reset();
            latestStatsBridgeResult = StatsBridgeLookupResult.unavailable();
        }
        traceStats(postStartRosterScheduled
            ? "game-world transition scheduled post-start roster"
            : "world reset clears pending automatic roster and latest result");
        PENDING_STATS_NOTICES.clear();
        PENDING_CONFIGURATION_NOTICES.clear();
        PENDING_MANUAL_STATS_RESULTS.clear();
        PREGAME_STATS_CHATTERS.clear();
        PREGAME_STATS_LOOKUP_ATTEMPTS.clear();
        PENDING_WHO_STATS_REFRESHES.clear();
        StatsBridgeClient client = statsBridgeClient;
        if (client != null) client.resetForNewWorld();
        if (active != null) {
            active.onWorldLoading();
            currentPresentation = null;
            VISIBLE_PLAYER_OBSERVATION_STARTED.set(false);
            NICKED_SESSION_PLAYER_IDS.clear();
            PENDING_NICK_NOTICES.clear();
            PREGAME_NICK_CHATTERS.clear();
            if (!postStartRosterScheduled) PENDING_TEAM_NICK_NOTICES.clear();
            NICK_OBSERVATION_LOGGED.set(false);
            MARKER_RENDER_LOGGED.set(false);
            TAB_RENDER_HOOK_LOGGED.set(false);
            NAME_RENDER_HOOK_LOGGED.set(false);
            NAME_RENDER_SUFFIX_DECISION_LOGGED.set(false);
            System.out.println("[HypixelLegitils] World lifecycle reset.");
        }
    }

    public static boolean isStarted() {
        return STARTED.get();
    }
}
