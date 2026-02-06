# 📡 Privacy Radar – Technical Brief

This document captures the technical blueprint for **Privacy Radar**, an always-on capability that surfaces suspicious permission usage inside SCAMYNX. It translates the product ask (“real-time monitoring” and “intelligent risk scoring”) into concrete Android components, storage models, and rollout steps.

> **Feature snapshot**
> - **Real-time monitoring of app permissions and sensitive resource access** (camera, microphone, location, contacts) keeps a running audit of high-value signals.
> - **Intelligent risk scoring with behavioral anomaly detection** compares live activity against historical baselines to highlight abnormal or abusive patterns.

## ⚙️ Core Capabilities
1. **Privacy Radar**
   - Real-time monitoring of app permissions and sensitive resource access (camera, microphone, location, contacts).
   - Intelligent risk scoring with behavioral anomaly detection.
2. **Anti-Phishing Link Analyzer**
   - On-device URL inspection with heuristic and reputation-based threat scoring.
   - Instant warning overlays in messaging apps and browsers.
3. **Wi-Fi Security Sentinel**
   - Network posture evaluation (encryption strength, captive portal, ARP/MITM indicators).
   - Safety recommendations tailored to public Wi-Fi environments.
4. **Password Strength & Breach Exposure Check**
   - Local entropy scoring and pattern recognition.
   - k-Anonymity breach prefix lookup (no password stored or transmitted).
5. **Social Engineering Scam Detector**
   - Lightweight NLP pattern detection for fraudulent messages and impersonation attempts.
   - Contextual guidance so users avoid tapping suspicious DMs, SMS, or emails.

### Real-time Monitoring Implementation
- **Event Sources**: Wrap `PackageManager`, `AppOpsManager`, `SensorPrivacyManager`, and `UsageStatsManager` in the `:data` layer so sensors can stream permission deltas and resource access in near real time without leaking Android SDK calls upward.
- **Streaming Flows**: Each sensor exposes a `Flow<PrivacyEvent>`; `privacyRadarCoordinator` merges them and tags events with the current screen/session context pulled from `Lifecycle` observers for a richer timeline.
- **Immediate Surfaces**: When a high-priority resource (camera/mic/location) fires, the coordinator pushes into `MutableSharedFlow` so UI tabs and notification workers can render the event before it hits Room.
- **Battery Guardrails**: Throttle low-value resources (e.g., contacts) to a 5 minute sampling window and fall back to WorkManager snapshots if the device enters `Doze` to satisfy the “always-on” promise without burning power.

### Intelligent Risk Scoring & Behavioral Anomaly Detection
- **Feature Engineering**: Persist per-app baselines (rolling 7/30-day stats) so the risk engine can compute Z-scores, visibility drift, and permission mismatches from a single DAO call.
- **Hybrid Detection**: Combine deterministic rules (e.g., camera in background > 2 minutes) with statistical anomalies; both feed the same `RiskResult` model so UI/components don’t care which path triggered the alert.
- **Confidence Narratives**: Attach short narratives plus a confidence bucket (`LOW/MED/HIGH`) to every anomaly; narratives concatenate resource, timeframe, and context (foreground/background, screen-off) for actionable messaging.
- **Feedback Loop**: Record “user dismissed” and “user confirmed risk” actions against the baseline entity; the nightly job decays scores for ignored anomalies so the system adapts to legitimate background-heavy apps.

---

## 🎯 Capability Goals
- **Real-time telemetry** of permission grants/denials and high-value resource access (camera, microphone, location, contacts, clipboard, sensors).
- **Behavior-aware risk scoring** that highlights apps whose runtime behavior deviates from historical patterns or expected platform usage.
- **Actionable surfacing** inside the app (timeline, per-app profile, and background alerts) without exfiltrating user data.

---

## 🧱 Architecture Overview
| Layer | Responsibility | Notes |
| --- | --- | --- |
| **Sensors** | Collect OS-level signals (permission state, AppOps events, SensorPrivacy toggles, UsageStats) | Lives in `:data` with wrappers to keep SDK calls isolated. |
| **Event Ingestor** | Normalizes raw signals into `PrivacyEvent` models and deduplicates bursts | Kotlin Coroutines `Flow`; merges with WorkManager for deferred jobs. |
| **Behavior Baselines** | Maintains rolling statistics per app + resource | Uses Room (`PrivacyEventEntity`, `PrivacyBaselineEntity`). |
| **Risk Engine** | Computes scores, anomaly flags, and narratives | Lives in `:domain`; pure functions so it’s testable. |
| **Surface Layer** | Compose UI cards, notifications, and optional Supabase sync | Feature-flagged via `BuildConfig.PRIVACY_RADAR_ENABLED`. |

```
Sensors → Event Ingestor → Baselines → Risk Engine → Surfaces/Alerts
```

---

## 🔍 Monitoring Pipeline

### 1. Permission Snapshot Watcher
- Triggered on app launch, app updates (PackageMonitor), and manual refresh.
- Reads `PackageManager` + `PermissionInfo` to build the ground-truth matrix (`granted`, `denied`, `restricted`).
- Emits `PermissionSnapshotEvent` so UI can show immediate inconsistencies (e.g., SMS granted to a wallpaper app).

### 2. Sensitive Resource Sensors
| Signal | Android API | Notes |
| --- | --- | --- |
| Camera/Mic usage | `SensorPrivacyManager`, `AppOpsManager.OP_CAMERA/MICROPHONE` | Works on Android 12+. Fallback to `CameraManager` callbacks. |
| Location | `LocationManager` + `AppOpsManager.OP_FINE_LOCATION` | Track both continuous access and sudden background pulls. |
| Contacts | `ContentObserver` on `ContactsContract.AUTHORITY` + recent `AppOpsManager.OP_READ_CONTACTS`. |
| Clipboard | `ClipboardManager.addPrimaryClipChangedListener`. |
| Foreground services | `UsageStatsManager` + `ActivityManager.getRunningServices`. Helps correlate sensor usage with actual UI visibility. |
| Wi‑Fi security posture | `ConnectivityManager`, `WifiManager`, `/proc/net/arp` | Derives encryption strength, captive portal status, and ARP/MITM heuristics. |

### 3. Wi‑Fi Security Sentinel
- **Snapshot & heuristics**: `WifiSecurityAnalyzer` samples the active Wi‑Fi network via `WifiManager`/`ConnectivityManager`, normalizes security type (OPEN/WPAx), and inspects `/proc/net/arp` for duplicate MAC/IP pairs signalling ARP spoofing.
- **Risk scoring**: `evaluateWifiSnapshot` blends encryption strength, captive-portal detection, metered state, signal RSSI, and ARP indicators into a 0‑1 score mapped onto `RiskCategory` for consistent handling with other Privacy Radar events.
- **Recommendations**: Each assessment carries localized recommendations (VPN usage, avoiding sensitive logins, forgetting the AP) so UI surfaces can present concrete remediation, especially on public Wi‑Fi.
- **Event integration**: `WifiSecurityEventSource` registers a `ConnectivityManager.NetworkCallback` for Wi‑Fi transports and publishes `PrivacyEvent` entries tagged as `WIFI_NETWORK`, enabling hot-lane alerts and timeline persistence alongside other sensors.

### 4. Password Strength & Breach Exposure Check
- **Entropy & pattern analysis**: `PasswordSecurityAnalyzer` runs entirely on-device, calculating entropy based on character sets, spotting sequential/keyboard patterns, and flagging dictionary-based strings before any network call occurs.
- **Privacy-preserving breach lookup**: The analyzer hashes the password with SHA‑1, sends only the first 5 characters (prefix) to `api.pwnedpasswords.com/range`, and scans the suffix list locally; the raw password or full hash never leaves the device.
- **Actionable guidance**: The resulting `PasswordSecurityReport` includes strength classification, entropy bits, breach counts, warnings, and normalized recommendations (length, variety, 2FA, password manager, forced resets if breached).
- **UX surfacing**: The Home screen shows a dedicated card with masked input, instant feedback, localized warnings, and breach badges so users can validate credentials before reusing them inside public or enterprise environments.

### 5. Social Engineering Scam Detector
- **On-device NLP heuristics**: `SocialEngineeringAnalyzer` tokenizes suspicious DMs/SMS, looks for urgency, authority threats, payment requests, impersonation cues, and obfuscated links, producing a normalized risk score without uploading the content.
- **Guided mitigation**: Detected indicators map to human-readable guidance (avoid gift card payments, verify through official apps, scan links before tapping) so users know the next safe step.
- **UI integration**: The Home screen exposes a new analyzer card where users can paste any message and instantly see risk gauges, flagged snippets, and recommended actions.

Events entering the pipeline contain:
- `packageName`
- `resourceType` (enum)
- `timestamp` / `duration`
- `visibilityContext` (foreground, background, screen-off)
- `confidence` (direct API vs inferred)

### 3. Event Ingestor & Storage
- Debounce bursts (e.g., continuous microphone usage) into a single window.
- Persist into Room (`PrivacyEventEntity`) with indices on `packageName` and `resourceType`.
- Maintain `PrivacyBaselineEntity`:
  - `averageDailyCount`
  - `medianDuration`
  - `lastSeenForegroundState`
  - `expectedVisibility` (foreground/background)

Baselines update via a nightly `WorkManager` job to keep compute off the hot path.

---

## 🧠 Risk Scoring & Anomaly Detection

### Feature Vector
For each `(packageName, resourceType)` tuple we snapshot a feature vector every time fresh events arrive:
- **Frequency Z-Score**: `(todayCount - avg7d) / std7d`. Rolling stats use Welford’s algorithm so we can stream updates without precision loss.
- **Duration Delta**: `todayDuration / medianDuration` capped at 3× to avoid a single runaway session skewing the model.
- **Visibility Drift**: multiplier > 1 when access happens while the app is backgrounded or the screen is off compared to a historically foreground baseline.
- **Permission Mismatch**: penalty if a dangerous permission stays granted but the correlated resource has < 1 use per 7 days (signal for silent staging or privilege hoarding).
- **User Overrides**: bonus risk when SensorPrivacy toggles, lockdown mode, or quick settings tiles explicitly disabled the resource but the app keeps polling.
- **Burstiness Index**: `maxWindowUsage / medianWindowUsage` computed on 15-minute windows to highlight sudden micro-spikes that traditional averages hide.

The vector is persisted in `PrivacyFeatureSnapshotEntity` so the domain layer can request historical slices (e.g., last 24h vs 7d) without recomputing from raw `PrivacyEvent` rows. All metrics are normalized to `0f..1f` before being fed to the scorer to keep weight tuning straightforward.

#### `PrivacyFeatureSnapshotEntity` schema
| Column | Type | Notes |
| --- | --- | --- |
| `id` | `PRIMARY KEY AUTOINCREMENT` | Synthetic row id for Room diffing. |
| `packageName` | `TEXT NOT NULL` | Canonical package id; stored lowercase for deterministic lookups. |
| `resourceType` | `INTEGER NOT NULL` | Enum ordinal matching `PrivacyResourceType`. |
| `windowStartEpochMillis` | `INTEGER NOT NULL` | Inclusive start of the aggregation window (defaults to midnight local). |
| `windowEndEpochMillis` | `INTEGER NOT NULL` | Exclusive end of the window; enables overlapping/rolling windows. |
| `frequencyZScore` | `REAL NOT NULL` | Normalized to `0f..1f`. |
| `durationRatio` | `REAL NOT NULL` | Clamped 0–3 then normalized. |
| `visibilityDrift` | `REAL NOT NULL` | 0 when behavior matches baseline, > 0.5 in background misuse cases. |
| `permissionMismatch` | `REAL NOT NULL` | Captures “granted-but-idle” penalties. |
| `overrideConflict` | `REAL NOT NULL` | Reflects SensorPrivacy/user override friction. |
| `burstinessIndex` | `REAL NOT NULL` | Highlights spiky behavior after normalization. |
| `snapshotScore` | `REAL NOT NULL` | Optional cached composite score; lets UI query sorted rows without re-running the engine. |
| `createdAtMillis` | `INTEGER NOT NULL` | Write timestamp for retention GC. |

Indices:
- `idx_feature_package_resource_window` (`packageName`, `resourceType`, `windowStartEpochMillis`) for fast retrieval of a single tuple’s history.
- `idx_feature_window_score` (`windowStartEpochMillis`, `snapshotScore` DESC) so the risk engine can pull top offenders per window without scanning the table.

Retention policy: keep rolling 30 days of windows per `(packageName, resourceType)` tuple, purge older rows in the nightly baseline job to cap storage.

### Formula (illustrative)
```
risk = clamp(
    0.25 * zScore.max(0f) +
    0.20 * durationRatio.max(0f) +
    0.25 * visibilityDrift +
    0.15 * permissionMismatch +
    0.15 * overrideConflict,
0f, 1f)
```

- **Behavioral anomaly** is flagged when `risk >= 0.7` OR any single metric crosses a “critical” threshold (e.g., camera used > 5 minutes in background).
- The engine also attaches a short narrative (`"Camera accessed in background for 6m by com.example.vpn"`).

---

## 📱 Surfacing & UX Hooks
- **Timeline Tab**: Chronological feed of `PrivacyEvent` entries with color-coded severity.
- **Per-App Sheet**: Aggregate stats, last 7 days sparkline, quick actions (open system App Info, revoke permission).
- **Alerts**: Heads-up notification + in-app banner when a new anomaly is detected. Relies on `NotificationManager` with channel `privacy_radar_alerts`.
- **Export**: Optional JSON export feeding into existing report pipeline (`:report` module) for enterprise triage.

Feature flagging:
```kotlin
if (BuildConfig.PRIVACY_RADAR_ENABLED) {
    privacyRadarCoordinator.start()
}
```
Set via Gradle property (`-PprivacyRadar=true`) or CI env var for controlled rollout.

---

## 🔐 Privacy & Security Guardrails
- All computation remains **on-device**; Supabase sync (if enabled) pushes only aggregated statistics with hashes of package names.
- No screenshots, audio recordings, or raw payloads are stored.
- Users can pause monitoring; we persist only counters needed to resume baselines.
- Respect Android policies: request `PACKAGE_USAGE_STATS`, `POST_NOTIFICATIONS`, and `FOREGROUND_SERVICE_SPECIAL_USE` (if using microphone background service) with clear consent screens.

---

## 🧪 Testing Strategy
- **Unit tests**: deterministic inputs for `RiskEngine` formulas and baseline updates.
- **Instrumentation**: Fake `AppOpsManager` via dependency injection; use Espresso to simulate grant/revoke flows.
- **Soak tests**: Run on companion device streaming random permission activity to validate battery impact (<2% per day goal).
- **Security review**: Ensure no additional logs or exports leak permission info.

---

## 🚀 Rollout Plan
1. **Phase 0 (internal)**: Hidden developer toggle in Settings → Diagnostics. Collect telemetry on CPU/Battery only.
2. **Phase 1 (beta)**: Enable alerts for camera/mic anomalies; log rest silently.
3. **Phase 2 (public)**: Turn on full resource set, integrate export + Supabase sync if enterprise tenant configured.

---

## 📌 Next Steps
- Define Room entities + DAO contracts (`PrivacyEventDao`, `PrivacyBaselineDao`).
- Implement sensor wrappers with fallback strategies per API level.
- Build Compose components for the Timeline tab.
- Wire feature flag to remote config (optional) for staged rollouts.

This brief should be referenced alongside `SupabaseModule.kt` and future data-layer PRDs to ensure Privacy Radar matures consistently with the rest of SCAMYNX.
