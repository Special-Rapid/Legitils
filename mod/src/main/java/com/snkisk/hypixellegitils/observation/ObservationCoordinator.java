package com.snkisk.hypixellegitils.observation;

import com.snkisk.hypixellegitils.alert.AlertPresentation;
import com.snkisk.hypixellegitils.alert.AlertSink;
import com.snkisk.hypixellegitils.alert.LocalAlertSink;
import com.snkisk.hypixellegitils.config.LegitilsConfig;
import com.snkisk.hypixellegitils.config.MarkerSettings;
import com.snkisk.hypixellegitils.config.MarkerHistoryEntry;
import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.evidence.Confidence;
import com.snkisk.hypixellegitils.evidence.Evidence;
import com.snkisk.hypixellegitils.evidence.EvidencePolicy;
import com.snkisk.hypixellegitils.evidence.EvidencePolicyContext;
import com.snkisk.hypixellegitils.evidence.PolicyDecision;
import com.snkisk.hypixellegitils.detection.DetectionEngine;
import com.snkisk.hypixellegitils.detection.BedNukeSignalCheck;
import com.snkisk.hypixellegitils.detection.NoBreakDelaySignalCheck;
import com.snkisk.hypixellegitils.detection.PlayerSample;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/** Java-only coordinator. Minecraft world/entity reads belong in later Mixin-owned adapters. */
public final class ObservationCoordinator {
    private static final int MAXIMUM_PLAYERS = 256;
    private static final long PLAYER_STALE_AFTER_MILLIS = 120000L;
    private LegitilsConfig config;
    private final PlayerObservationStore players = new PlayerObservationStore(MAXIMUM_PLAYERS, PLAYER_STALE_AFTER_MILLIS);
    private final EvidencePolicy evidencePolicy = new EvidencePolicy();
    private final DetectionEngine detectionEngine;
    private final BedNukeSignalCheck bedNuke = new BedNukeSignalCheck();
    private final NoBreakDelaySignalCheck noBreakDelay = new NoBreakDelaySignalCheck();
    private final AcceptedAlertMarkers acceptedMarkers = new AcceptedAlertMarkers();
    private final AlertSink alertSink;
    private final MarkerHistoryPersistence markerHistoryPersistence;
    private UUID developmentSelfPlayerId;
    private boolean globalLag;
    private boolean worldTransition;

    public ObservationCoordinator(LegitilsConfig config, AlertSink alertSink) {
        this(config, alertSink, Collections.<UUID, MarkerHistoryEntry>emptyMap(), new MarkerHistoryPersistence() {
            @Override
            public void write(Map<UUID, MarkerHistoryEntry> history) {
                // Unit and in-memory callers intentionally keep history local to this coordinator.
            }
        });
    }

    public ObservationCoordinator(
        LegitilsConfig config,
        AlertSink alertSink,
        Map<UUID, MarkerHistoryEntry> markerHistory,
        MarkerHistoryPersistence markerHistoryPersistence
    ) {
        this.config = config;
        this.alertSink = alertSink;
        this.detectionEngine = new DetectionEngine(config);
        this.markerHistoryPersistence = markerHistoryPersistence;
        acceptedMarkers.restore(markerHistory);
    }

    /** Applies only a persisted detector-set change and clears partial timing patterns. */
    public synchronized void applyRuntimeDetectorConfig(LegitilsConfig updated) {
        if (updated == null || !sameNonDetectorSettings(config, updated)) {
            throw new IllegalArgumentException("Runtime update may only change the detector set");
        }
        if (config.enabledDetectors.equals(updated.enabledDetectors)) return;
        config = updated;
        detectionEngine.resetForObservationDiscontinuity();
        bedNuke.reset();
        noBreakDelay.reset();
    }

    private static boolean sameNonDetectorSettings(LegitilsConfig left, LegitilsConfig right) {
        return left.sensitivity == right.sensitivity
            && left.notifications.chatEnabled == right.notifications.chatEnabled
            && left.notifications.overlayEnabled == right.notifications.overlayEnabled
            && left.notifications.soundEnabled == right.notifications.soundEnabled
            && left.normalCooldownMillis == right.normalCooldownMillis
            && left.airStallCooldownMillis == right.airStallCooldownMillis
            && left.debugEnabled == right.debugEnabled
            && left.markerSettings.sameAs(right.markerSettings)
            && left.nickDetectionSettings.sameAs(right.nickDetectionSettings);
    }

    /** Applies only persisted local marker settings; local marker history remains unchanged. */
    public synchronized void applyRuntimeMarkerConfig(LegitilsConfig updated) {
        if (updated == null || !sameExceptMarkers(config, updated)) {
            throw new IllegalArgumentException("Runtime marker update may only change marker settings");
        }
        if (config.markerSettings.sameAs(updated.markerSettings)) return;
        config = updated;
        if (config.markerSettings.enabled) promoteAutoBlacklists(System.currentTimeMillis());
    }

    private static boolean sameExceptMarkers(LegitilsConfig left, LegitilsConfig right) {
        return left.sensitivity == right.sensitivity
            && left.enabledDetectors.equals(right.enabledDetectors)
            && left.notifications.chatEnabled == right.notifications.chatEnabled
            && left.notifications.overlayEnabled == right.notifications.overlayEnabled
            && left.notifications.soundEnabled == right.notifications.soundEnabled
            && left.normalCooldownMillis == right.normalCooldownMillis
            && left.airStallCooldownMillis == right.airStallCooldownMillis
            && left.debugEnabled == right.debugEnabled
            && left.nickDetectionSettings.sameAs(right.nickDetectionSettings);
    }

    /** Applies only the persisted development self-observation setting. */
    public synchronized void applyRuntimeDevelopmentConfig(LegitilsConfig updated) {
        if (updated == null || !sameExceptDevelopment(updated, config)) {
            throw new IllegalArgumentException("Runtime development update may only change the self-observation setting");
        }
        if (config.debugEnabled == updated.debugEnabled) return;
        config = updated;
        if (!config.debugEnabled) developmentSelfPlayerId = null;
    }

    private static boolean sameExceptDevelopment(LegitilsConfig left, LegitilsConfig right) {
        return left.sensitivity == right.sensitivity
            && left.enabledDetectors.equals(right.enabledDetectors)
            && left.notifications.chatEnabled == right.notifications.chatEnabled
            && left.notifications.overlayEnabled == right.notifications.overlayEnabled
            && left.notifications.soundEnabled == right.notifications.soundEnabled
            && left.normalCooldownMillis == right.normalCooldownMillis
            && left.airStallCooldownMillis == right.airStallCooldownMillis
            && left.markerSettings.sameAs(right.markerSettings)
            && left.nickDetectionSettings.sameAs(right.nickDetectionSettings);
    }

    /** Keeps the running configuration coherent after an immediate Nick-display update. */
    public synchronized void applyRuntimeNickDetectionConfig(LegitilsConfig updated) {
        if (updated == null || !sameExceptNickDetection(updated, config)) {
            throw new IllegalArgumentException("Runtime Nick update may only change the Nick display setting");
        }
        if (config.nickDetectionSettings.sameAs(updated.nickDetectionSettings)) return;
        config = updated;
    }

    private static boolean sameExceptNickDetection(LegitilsConfig left, LegitilsConfig right) {
        return left.sensitivity == right.sensitivity
            && left.enabledDetectors.equals(right.enabledDetectors)
            && left.notifications.chatEnabled == right.notifications.chatEnabled
            && left.notifications.overlayEnabled == right.notifications.overlayEnabled
            && left.notifications.soundEnabled == right.notifications.soundEnabled
            && left.normalCooldownMillis == right.normalCooldownMillis
            && left.airStallCooldownMillis == right.airStallCooldownMillis
            && left.debugEnabled == right.debugEnabled
            && left.markerSettings.sameAs(right.markerSettings);
    }

    /** Applies only local alert delivery settings without resetting detection state. */
    public synchronized void applyRuntimeNotificationConfig(LegitilsConfig updated) {
        if (updated == null || !sameExceptNotifications(config, updated)) {
            throw new IllegalArgumentException("Runtime notification update may only change notification settings");
        }
        if (sameNotifications(config, updated)) return;
        if (!(alertSink instanceof LocalAlertSink)) {
            throw new IllegalStateException("Runtime notification update requires LocalAlertSink");
        }
        config = updated;
        ((LocalAlertSink) alertSink).setNotificationSettings(updated.notifications);
    }

    private static boolean sameExceptNotifications(LegitilsConfig left, LegitilsConfig right) {
        return left.sensitivity == right.sensitivity
            && left.enabledDetectors.equals(right.enabledDetectors)
            && left.normalCooldownMillis == right.normalCooldownMillis
            && left.airStallCooldownMillis == right.airStallCooldownMillis
            && left.debugEnabled == right.debugEnabled
            && left.markerSettings.sameAs(right.markerSettings)
            && left.nickDetectionSettings.sameAs(right.nickDetectionSettings);
    }

    private static boolean sameNotifications(LegitilsConfig left, LegitilsConfig right) {
        return left.notifications.chatEnabled == right.notifications.chatEnabled
            && left.notifications.overlayEnabled == right.notifications.overlayEnabled
            && left.notifications.soundEnabled == right.notifications.soundEnabled;
    }

    public synchronized void observePlayer(UUID playerId, long nowMillis) {
        players.observe(playerId, nowMillis);
        acceptedMarkers.observe(playerId, nowMillis);
        worldTransition = false;
    }

    public synchronized PolicyDecision submit(Evidence evidence, long nowMillis, boolean sufficientHistory) {
        return submit(evidence, nowMillis, sufficientHistory, globalLag);
    }

    private PolicyDecision submit(Evidence evidence, long nowMillis, boolean sufficientHistory, boolean evidenceGlobalLag) {
        PolicyDecision decision = evidencePolicy.evaluate(
            evidence,
            new EvidencePolicyContext(nowMillis, evidenceGlobalLag, worldTransition, sufficientHistory),
            config,
            isDevelopmentSelfPlayer(evidence.playerId) && config.debugEnabled
        );
        if (decision.shouldAlert) {
            alertSink.accept(evidence, nowMillis);
            if (config.markerSettings.enabled && !isNickedProfile(evidence.playerId) && !isDevelopmentSelfPlayer(evidence.playerId)) {
                // Detection clocks can be world-tick based; persistent history uses real epoch time.
                updateMarkerHistory(evidence.playerId, System.currentTimeMillis(), MarkerMutation.ACCEPTED);
            }
        }
        return decision;
    }

    /** Development samples may alert locally but must never create persistent Blacklist history. */
    public synchronized void setDevelopmentSelfPlayerId(UUID playerId) {
        developmentSelfPlayerId = playerId;
    }

    private boolean isDevelopmentSelfPlayer(UUID playerId) {
        return playerId != null && playerId.equals(developmentSelfPlayerId);
    }

    public synchronized void setGlobalLag(boolean globalLag) {
        this.globalLag = globalLag;
    }

    /**
     * Called at the start of a delayed client tick, before queued network/world
     * updates can resemble an immediate mining cadence.
     */
    public synchronized void onImmediateGlobalLag() {
        globalLag = true;
        detectionEngine.resetForObservationDiscontinuity();
        bedNuke.reset();
        noBreakDelay.reset();
    }

    /** Begins a single client observation frame; all evidence in a lagged frame is suppressed. */
    public synchronized void beginObservationFrame(boolean globalLag) {
        this.globalLag = globalLag;
        if (globalLag) {
            detectionEngine.resetForObservationDiscontinuity();
            noBreakDelay.reset();
        }
        // A normal client frame with an attached world/player is the only
        // adapter-owned signal that the post-load transition has stabilised.
        if (!globalLag) worldTransition = false;
    }

    /** Does not alter policy/alerts; it only prevents timing state crossing an ambiguous frame. */
    public synchronized void onObservationDiscontinuity() {
        detectionEngine.resetForObservationDiscontinuity();
        noBreakDelay.reset();
    }

    /** Accepts one complete visible-player frame and invalidates missing-player history. */
    public synchronized void observeFrame(List<PlayerSample> samples) {
        java.util.Set<UUID> observed = new java.util.HashSet<UUID>();
        if (samples != null) {
            for (PlayerSample sample : samples) if (sample != null) observed.add(sample.playerId);
        }
        detectionEngine.retainOnlyPlayers(observed);
        if (samples != null) for (PlayerSample sample : samples) observe(sample);
    }

    /** Accepts only immutable adapter output and routes every detector result through EvidencePolicy. */
    public synchronized void observe(PlayerSample sample) {
        if (sample == null) return;
        observePlayer(sample.playerId, sample.observedAtMillis);
        // A lagged frame may retain visibility bookkeeping, but it must never
        // become the first tick of a detector timing sequence.
        if (globalLag) return;
        List<Evidence> produced = detectionEngine.observe(sample, isDevelopmentSelfPlayer(sample.playerId) && config.debugEnabled);
        for (Evidence evidence : produced) submit(evidence, sample.observedAtMillis, true);
    }

    public synchronized AlertPresentation onClientTick(long nowMillis) {
        players.pruneExpired(nowMillis);
        detectionEngine.pruneExpired(nowMillis);
        Evidence bedNukeEvidence = bedNuke.evaluate(nowMillis);
        if (bedNukeEvidence != null) submit(bedNukeEvidence, nowMillis, true);
        return alertSink.presentation(nowMillis);
    }

    /** Adapter entry point for a fully loaded, bounded pre-break bed snapshot. */
    public synchronized void observeBedStructure(BedNukeSignalCheck.BedStructure structure, long nowMillis) {
        bedNuke.register(structure, nowMillis, true);
    }

    /** Adapter entry point for a server-applied state in a registered bed volume. */
    public synchronized void observeBedBlockState(BedNukeSignalCheck.BlockPosition position, BedNukeSignalCheck.BlockKind state, long nowMillis) {
        bedNuke.observeBlockState(position, state, nowMillis);
    }

    /** Records only actor-resolved mining progress from the network adapter. */
    public synchronized void observeNoBreakDelayProgress(NoBreakDelaySignalCheck.Progress progress) {
        Evidence evidence = noBreakDelay.observeProgress(progress);
        if (evidence != null) submit(evidence, evidence.observedAtMillis, true);
    }

    /** Confirms a candidate mining completion only after its server block removal. */
    public synchronized void observeNoBreakDelayBlockRemoval(NoBreakDelaySignalCheck.BlockPosition position, long worldTick, boolean completeContext) {
        Evidence evidence = noBreakDelay.observeBlockRemoval(position, worldTick, completeContext);
        if (evidence != null) submit(evidence, evidence.observedAtMillis, true);
    }

    /** Development-only local controller observation; it remains excluded from persistent Blacklist history. */
    public synchronized void observeDevelopmentNoBreakDelay(UUID playerId, long worldTick, int blockHitDelay, boolean breakCompleted) {
        if (!isDevelopmentSelfPlayer(playerId)) return;
        Evidence evidence = noBreakDelay.observeLocalPostBreakDelay(playerId, worldTick, blockHitDelay, breakCompleted);
        if (evidence != null) submit(evidence, evidence.observedAtMillis, true);
    }

    /** Dev-only direct test signal for an observing-client F3+T-style tick stall. */
    public synchronized void observeDevelopmentTimerStall(UUID playerId, long nowMillis) {
        if (!isDevelopmentSelfPlayer(playerId) || nowMillis < 0L) return;
        Evidence evidence = new Evidence(
            DetectorId.AIR_STALL,
            playerId,
            Confidence.LOW,
            nowMillis,
            "development client-tick stall"
        );
        // This evidence is a requested self-test result, not an inference about
        // remote players. Keep detector enablement, world transition, cooldown,
        // self-marker, and self-WDR rules, bypassing only global-lag suppression.
        submit(evidence, nowMillis, true, false);
    }

    /** Chunk replacement makes any stored cuboid history unreliable. */
    public synchronized void onChunkTransition() {
        bedNuke.reset();
        noBreakDelay.reset();
    }

    public synchronized void onWorldLoading() {
        players.reset();
        evidencePolicy.reset();
        detectionEngine.reset();
        bedNuke.reset();
        noBreakDelay.reset();
        alertSink.reset();
        globalLag = false;
        worldTransition = true;
        developmentSelfPlayerId = null;
    }

    public synchronized int observedPlayerCount() {
        return players.size();
    }

    public synchronized int detectorStateCount() {
        return detectionEngine.size();
    }

    /** Read-only local blacklist query; callers must additionally prove the entity is currently visible. */
    public synchronized boolean shouldShowAcceptedAlertMarker(UUID playerId, long nowMillis) {
        MarkerSettings markers = config.markerSettings;
        return markers.enabled && acceptedMarkers.isBlacklisted(playerId, nowMillis);
    }

    /** Adds a local-only manual marker for a UUID that is currently visible to the chat adapter. */
    public synchronized boolean blacklistMarker(UUID playerId, long nowMillis) {
        if (isNickedProfile(playerId)) return false;
        return updateMarkerHistory(playerId, nowMillis, MarkerMutation.BLACKLIST);
    }

    /** Removes a local marker entry, including any accepted-alert count and blacklist state. */
    public synchronized boolean removeMarker(UUID playerId) {
        if (playerId == null) return false;
        Map<UUID, MarkerHistoryEntry> previous = acceptedMarkers.snapshot();
        if (!acceptedMarkers.remove(playerId)) return false;
        return persistMarkerHistoryOrRestore(previous);
    }

    /** Clears every local marker-history entry; it has no server-side effect. */
    public synchronized boolean clearAllMarkers() {
        Map<UUID, MarkerHistoryEntry> previous = acceptedMarkers.snapshot();
        if (!acceptedMarkers.clearAll()) return false;
        return persistMarkerHistoryOrRestore(previous);
    }

    public synchronized int markerHistoryCount() {
        return acceptedMarkers.size();
    }

    public synchronized int blacklistedMarkerCount() {
        return acceptedMarkers.blacklistedCount();
    }

    /** Stores a Mojang-resolved cache name only for an existing non-nick local Blacklist entry. */
    public synchronized void recordMojangResolvedName(UUID playerId, String name, long nowMillis) {
        if (isNickedProfile(playerId) || !acceptedMarkers.isBlacklisted(playerId, nowMillis)) return;
        Map<UUID, MarkerHistoryEntry> previous = acceptedMarkers.snapshot();
        if (acceptedMarkers.recordMojangResolvedName(playerId, name, nowMillis)) persistMarkerHistoryOrRestore(previous);
    }

    /** Stores a server-presented non-nick display name only when it changed; never writes nick aliases. */
    public synchronized void recordObservedServerName(UUID playerId, String name, long nowMillis) {
        if (isNickedProfile(playerId) || !acceptedMarkers.isBlacklisted(playerId, nowMillis)) return;
        Map<UUID, MarkerHistoryEntry> previous = acceptedMarkers.snapshot();
        if (acceptedMarkers.recordObservedServerName(playerId, name, nowMillis)) persistMarkerHistoryOrRestore(previous);
    }

    /** Read-only local history for paginated command output. */
    public synchronized Map<UUID, MarkerHistoryEntry> markerHistory() {
        return acceptedMarkers.snapshot();
    }

    private boolean updateMarkerHistory(UUID playerId, long nowMillis, MarkerMutation mutation) {
        if (playerId == null) return false;
        Map<UUID, MarkerHistoryEntry> previous = acceptedMarkers.snapshot();
        boolean changed = mutation == MarkerMutation.ACCEPTED
            ? acceptedMarkers.recordAccepted(playerId, nowMillis, config.markerSettings.threshold)
            : acceptedMarkers.blacklist(playerId, nowMillis);
        if (!changed) return false;
        return persistMarkerHistoryOrRestore(previous);
    }

    private boolean persistMarkerHistoryOrRestore(Map<UUID, MarkerHistoryEntry> previous) {
        try {
            markerHistoryPersistence.write(acceptedMarkers.snapshot());
            return true;
        } catch (Exception ignored) {
            acceptedMarkers.restore(previous);
            return false;
        }
    }

    private void promoteAutoBlacklists(long nowMillis) {
        Map<UUID, MarkerHistoryEntry> previous = acceptedMarkers.snapshot();
        if (acceptedMarkers.promoteEligible(config.markerSettings.threshold, nowMillis)) {
            persistMarkerHistoryOrRestore(previous);
        }
    }

    private enum MarkerMutation {
        ACCEPTED,
        BLACKLIST
    }

    private static boolean isNickedProfile(UUID playerId) {
        return playerId != null && playerId.version() == 1;
    }

    /** Returns a local, on-demand status summary; it is never rendered persistently. */
    public synchronized String statusText() {
        int active = 0;
        int available = 0;
        for (com.snkisk.hypixellegitils.config.DetectorId detector : com.snkisk.hypixellegitils.config.DetectorId.values()) {
            if (!detector.isImplementedInCurrentBuild()) continue;
            available++;
            if (config.isDetectorEnabled(detector)) active++;
        }
        return "anti-cheat " + active + "/" + available + " detectors active";
    }
}
