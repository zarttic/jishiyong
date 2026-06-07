# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android app. Core Gradle configuration lives in `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, and `app/build.gradle.kts`.

Application code is under `app/src/main/java/com/jishiyong/`:

- `data/db`, `data/repository`: Room entities, DAO, converters, database, and repository logic.
- `ui/screens`, `ui/components`, `ui/theme`: Jetpack Compose screens, reusable UI, and Material 3 theme.
- `viewmodel`: `AndroidViewModel` classes for screen state and data operations.
- `notification`: WorkManager reminder worker and notification helpers.
- `update`, `util`: GitHub release update checks and shared utilities.

Resources are in `app/src/main/res/`, including launcher assets, drawables, XML config, strings, and themes. GitHub Actions workflows are in `.github/workflows/`.

## Build, Test, and Development Commands

- `./gradlew assembleDebug`: builds a local debug APK.
- `./gradlew assembleRelease -PVERSION_NAME=2.4.2 -PVERSION_CODE=242`: builds a release APK; signing requires `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`.
- `./gradlew test`: runs JVM unit tests when present.
- `./gradlew connectedAndroidTest`: runs instrumentation tests on a connected device or emulator.
- `gh workflow run release.yml --ref main -f version_name=X.Y.Z -f version_code=N -f release_notes="..."`: publishes a signed GitHub Release through Actions.

Use GitHub Actions for authoritative APK builds when local Android SDK tooling is unavailable or architecture-incompatible.

Current local environment is `aarch64`. Gradle downloads an x86-64 `aapt2` binary that cannot execute on this machine, so Android build/test verification must be run through GitHub workflows instead of local Gradle commands.

Podman is available, but it is not by itself a fix for the local `aapt2` incompatibility. Cross-architecture container verification requires both a pullable amd64 image and x86-64 user-mode emulation (`qemu-x86_64` with binfmt registration, or an equivalent wrapper). As of 2026-06-07 on this machine:

- Docker Hub pulls may time out.
- `/proc/sys/fs/binfmt_misc` has no x86-64 interpreter registered.
- The available openEuler `qemu-user` / `qemu-user-static` packages provide arm/riscv interpreters here, not `qemu-x86_64`.
- The installed Android SDK `aapt2` binaries are x86-64 and fail with `Exec format error`.

Do not spend time retrying local Gradle Android build/test/schema generation unless those prerequisites have changed. Use GitHub Actions or an x86-64 Android SDK environment for authoritative `testDebugUnitTest`, `assembleDebug`, `assembleRelease`, and Room schema generation.

## Coding Style & Naming Conventions

Use Kotlin with 4-space indentation and the official Kotlin style. Keep Compose functions in PascalCase, state variables in lower camelCase, and constants in upper snake case where appropriate. Prefer existing MVVM and repository patterns over new abstractions. Keep user-facing strings in resources when they may need localization.

## Testing Guidelines

Add unit tests under `app/src/test/` and instrumentation/UI tests under `app/src/androidTest/`. Name test files after the class or feature under test, for example `ListConverterTest.kt` or `HomeScreenTest.kt`. Focus tests on Room converters, repository behavior, date logic, and startup-safe error handling.

## Commit & Pull Request Guidelines

History uses concise imperative commits, often prefixed with `fix:` or `feat:`. Examples: `fix: harden startup against local data errors`, `feat: 应用内检测升级功能`.

PRs should include a short summary, risk notes, screenshots for UI changes, and build/test evidence. Link related issues when available. Do not commit local APK outputs, keystores, secrets, or downloaded artifacts such as `output/`.

## Security & Configuration Tips

Never store signing keys or GitHub tokens in the repository. Configure release secrets in GitHub Actions. Keep `applicationId` aligned with the uploaded APK package name: `com.jishiyong`.
