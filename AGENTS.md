# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android app. Core Gradle configuration lives in `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, and `app/build.gradle.kts`.

Application code is under `app/src/main/java/com/jishiyong/`:

- `data/db`, `data/repository`: Room entities, DAO, converters, database, and repository logic.
- `agent`, `agent/llm`: voice inventory action parsing, matching, confirmation execution, memory, and OpenAI-compatible LLM planning.
- `speech`: local recording plus Baidu cloud ASR configuration and client code.
- `ui/screens`, `ui/components`, `ui/theme`: Jetpack Compose screens, reusable UI, and Material 3 theme.
- `viewmodel`: `AndroidViewModel` classes for screen state and data operations.
- `notification`: WorkManager reminder worker and notification helpers.
- `update`, `util`: GitHub release update checks and shared utilities.

Resources are in `app/src/main/res/`, including launcher assets, drawables, XML config, strings, and themes. GitHub Actions workflows are in `.github/workflows/`.

## Build, Test, and Development Commands

### GitHub Operations

Default to GitHub CLI (`gh`) for GitHub-side operations such as inspecting refs, dispatching workflows, watching runs, viewing logs, creating or merging PRs, and checking release state.

When repository configuration, build/release workflow inputs, required secrets, local environment constraints, or operational defaults change, update this `AGENTS.md` file in the same change so future agents use the current process.

The `main` branch is protected. Required status checks must be up to date and the `build` GitHub Actions check must pass; administrators are included in the protection rule. Do not bypass this by pushing directly to `main`.

### Current Machine: Use GitHub Actions

Current local environment is `aarch64`. Gradle downloads an x86-64 `aapt2` binary that cannot execute on this machine, so Android build/test verification must be run through GitHub workflows instead of local Gradle commands.

- `gh workflow run build.yml --ref "$(git branch --show-current)"`: run the authoritative debug build, JVM unit tests, lint, and Room schema check for the current branch on GitHub-hosted x86-64 runners.
- `gh workflow run build.yml --ref "$(git branch --show-current)" -f run_real_llm_smoke=true`: run the build workflow plus the real LLM smoke test for the current branch when `AI_API_KEY` is configured.
- `gh workflow run llm-smoke.yml --ref main -f ai_api_base_url="https://api.edgefn.net/v1" -f ai_model_name="DeepSeek-V3.2" -f recognized_text="今天喝了一瓶蒙牛纯牛奶"`: run only the real LLM smoke workflow.
- `gh run watch`: follow the latest workflow run to completion.
- `gh run view --log-failed`: inspect failed workflow logs.
- `gh workflow run release.yml --ref main -f version_name=X.Y.Z -f version_code=N -f release_notes="..."`: publish a signed GitHub Release through Actions.

Use GitHub Actions for authoritative APK builds when local Android SDK tooling is unavailable or architecture-incompatible. Do not spend time retrying local Gradle Android build/test/schema generation unless the prerequisites below have changed.

### x86-64 Android SDK Environment

Podman is available, but it is not by itself a fix for the local `aapt2` incompatibility. Cross-architecture container verification requires both a pullable amd64 image and x86-64 user-mode emulation (`qemu-x86_64` with binfmt registration, or an equivalent wrapper). As of 2026-06-07 on this machine:

- Docker Hub pulls may time out.
- `/proc/sys/fs/binfmt_misc` has no x86-64 interpreter registered.
- The available openEuler `qemu-user` / `qemu-user-static` packages provide arm/riscv interpreters here, not `qemu-x86_64`.
- The installed Android SDK `aapt2` binaries are x86-64 and fail with `Exec format error`.

These commands are appropriate on GitHub-hosted runners or another x86-64 Android SDK 35 / AGP 8.6 / Gradle 8.7 environment:

- `./gradlew testDebugUnitTest`: runs JVM unit tests for the Android debug variant.
- `./gradlew assembleDebug`: builds a local debug APK.
- `./gradlew assembleRelease -PVERSION_NAME=2.4.2 -PVERSION_CODE=242`: builds a release APK against Android SDK 35; signing requires `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`.
- `./gradlew connectedAndroidTest`: runs instrumentation tests on a connected device or emulator.

## Coding Style & Naming Conventions

Use Kotlin with 4-space indentation and the official Kotlin style. Keep Compose functions in PascalCase, state variables in lower camelCase, and constants in upper snake case where appropriate. Prefer existing MVVM and repository patterns over new abstractions. Keep user-facing strings in resources when they may need localization.

## Testing Guidelines

Add unit tests under `app/src/test/` and instrumentation/UI tests under `app/src/androidTest/`. Name test files after the class or feature under test, for example `ListConverterTest.kt` or `HomeScreenTest.kt`. Focus tests on Room converters, repository behavior, date logic, and startup-safe error handling.

For agent work, cover parser, planner, executor, matcher, memory, LLM JSON parsing, and failure/clarification paths under `app/src/test/java/com/jishiyong/agent/`. Keep real provider calls out of normal unit tests; use `llm-smoke.yml` or `build.yml` with `run_real_llm_smoke=true` for the real LLM smoke test.

Room schema generation happens as part of the x86-64 build/test flow. The build workflow uploads `room-schemas`; missing `app/schemas` or an empty schema directory fails CI. If `app/schemas` changes, download the artifact, inspect the schema diff, and commit intentional schema updates with the migration. Do not commit generated APKs or temporary workflow artifacts. Build and release workflows also upload test and lint reports on every run.

## Commit & Pull Request Guidelines

History uses concise imperative commits, often prefixed with `fix:` or `feat:`. Examples: `fix: harden startup against local data errors`, `feat: 应用内检测升级功能`.

PRs should include a short summary, risk notes, screenshots for UI changes, and build/test evidence. Link related issues when available. Do not commit local APK outputs, keystores, secrets, or downloaded artifacts such as `output/`.

## Security & Configuration Tips

Never store signing keys, API keys, GitHub tokens, or downloaded keystores in the repository. Configure release and smoke-test secrets in GitHub Actions:

- Release signing: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
- Baidu ASR release config: `BAIDU_ASR_APP_ID`, `BAIDU_ASR_API_KEY`, `BAIDU_ASR_SECRET_KEY`.
- Real LLM smoke tests: `AI_API_KEY`.

Keep `applicationId` aligned with the uploaded APK package name: `com.jishiyong`.
