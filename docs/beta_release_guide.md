# SCAMYNX Beta Launch Playbook

Helping future‑me finish the current refactor, validate it, and ship a polished beta.

---

## 1. Immediate TODOs (pre-debug)

- [ ] **Restore missing DTO usages**  
  The debug build currently fails because `ScanRepositoryImpl.kt` references `VirusTotalSubmitRequestDto`, `UrlScanResultDto`, and `pow()` without proper imports.  
  - Confirm `data/network/model/VirusTotalDtos.kt` exports the submit DTO and add the missing import in `ScanRepositoryImpl`.  
  - Create or rewire the URLScan result model in `UrlScanDtos.kt`, then update the repository layer to use it.  
  - Replace the bare `pow` call with `kotlin.math.pow` (or move the scoring helper into `RiskScorer`).  

- [ ] **Re-run Gradle sync + type check**  
  `./gradlew :app:compileDebugKotlin` should pass once the DTO wiring is fixed.

- [ ] **Verify new UI strings**  
  Review the freshly added strings under `app/src/main/res/values*/strings.xml` and ensure translations/localisations read correctly in both EN & FA.

---

## 2. Debug & Quality Pass

1. **Kotlin compilation**  
   ```bash
   ./gradlew :app:compileDebugKotlin
   ```
   Confirms all modules build after fixing the DTO gap.

2. **Static analysis / lint**  
   ```bash
   ./gradlew :app:lintDebug
   ```
   Catch UI accessibility or styling regressions introduced by the redesign.

3. **Unit & instrumentation**  
   ```bash
   ./gradlew testDebugUnitTest
   ./gradlew connectedDebugAndroidTest
   ```
   Ensure existing test coverage still passes; add coverage around `RiskScorer` if new logic lands there.

4. **Manual smoke on device/emulator**
   - Validate the new home hero gradient, scan modes, and feature cards.
   - Run a URL scan to confirm VirusTotal/UrlScan flows return without crashes.
   - Confirm the results screen risk meter/linear progress renders for each risk tier.

> Tip: Keep Android Studio Layout Inspector open to check runtime colors & typography against the design palette.

---

## 3. Pre-Beta Polish

- Update release notes (English & Persian) covering: hero redesign, results risk meter, feature highlights.
- Revisit `docs/THEME_IMPROVEMENTS.md` to reflect the new gradients and spacing tokens.
- Snap fresh screenshots for the Play listing (light & dark themes showcasing the hero and results cards).

---

## 4. Beta Release Checklist

1. **Version bump**  
   - Increment `versionCode` & `versionName` in `app/build.gradle.kts`.
   - Tag the release branch (e.g., `beta/v0.9.0`).

2. **Assemble signed artifact**  
   ```bash
   ./gradlew :app:bundleRelease
   ```
   or generate an `.aab` using Android Studio. Keep keystore + passwords handy.

3. **Final validation on release build**
   - Install the `release` APK locally.
   - Re-run primary flows (scan URL, file input, history navigation).

4. **Play Console rollout**
   - Upload the generated `.aab`.
   - Populate beta release notes (mirror the release docs).
   - Select the beta track audience and start rollout.

5. **Post-release monitoring**
   - Watch Firebase Crashlytics / Play Console vitals.
   - Gather user feedback on the upgraded UI and risk presentation.

---

## 5. Nice-to-Have Follow-ups

- Add integration tests that mock VirusTotal/UrlScan responses for deterministic scoring.
- Document the gradient system inside `docs/THEME_QUICK_REFERENCE.md`.
- Explore feature flags for toggling the new hero layout if rapid rollback is ever needed.

Happy shipping! 🚀
