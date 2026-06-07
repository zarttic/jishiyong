# x86 Development Migration

This document records what needs to move when switching development from the current `aarch64` machine to an x86-64 server.

## Why Move

The current local machine is `aarch64`. Android Gradle tasks download x86-64 Android SDK tools such as `aapt2`, and those binaries cannot run here without working x86-64 user-mode emulation. The current machine does not have the required `qemu-x86_64` / binfmt setup, so local Android build, test, and Room schema generation are not authoritative here.

An x86-64 server can run the normal Android Gradle workflow directly:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease -PVERSION_NAME=X.Y.Z -PVERSION_CODE=N
```

## Sync From GitHub

For committed project state, clone or pull `main` from GitHub:

```bash
git clone https://github.com/zarttic/jishiyong.git
cd jishiyong
git checkout main
git pull --ff-only
```

The Room schema baseline is already committed under `app/schemas/`, so a fresh x86-64 checkout does not need any extra schema bootstrap file.

## Local Content To Review

Before leaving the old machine, check for untracked or uncommitted files:

```bash
git status --short
```

As of 2026-06-07, the only known untracked directory on the current machine is:

```text
prototypes/
```

Handle it explicitly:

- If it contains useful experimental notes or source files, copy it separately or commit only the parts that should belong to the project.
- If it is disposable local work, do not migrate it.

Do not migrate generated build output:

```text
app/build/
.gradle/
build/
output/
*.apk
*.aab
```

## Do Not Sync Secrets

Do not copy these into Git or shared server storage:

```text
local.properties
*.jks
*.keystore
GitHub tokens
AI_API_KEY
release signing passwords
```

`local.properties` should be regenerated on the x86-64 server with that machine's Android SDK path, for example:

```properties
sdk.dir=/opt/android-sdk
```

Release signing should continue to use GitHub Actions secrets when possible. If local release builds are needed, provide signing values through environment variables only:

```bash
export ANDROID_KEYSTORE_PATH=/secure/path/release.keystore
export ANDROID_KEYSTORE_PASSWORD=...
export ANDROID_KEY_ALIAS=...
export ANDROID_KEY_PASSWORD=...
```

## x86-64 Server Prerequisites

Install or configure:

- JDK 17.
- Android SDK 34.
- Android Build Tools compatible with AGP 8.2.2.
- Git.
- GitHub CLI, if the server will trigger or inspect Actions.

Recommended environment variables:

```bash
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

After installing command line tools, accept SDK licenses:

```bash
sdkmanager --licenses
```

Install expected SDK packages if needed:

```bash
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

## First Verification On x86-64

Run:

```bash
./gradlew tasks --all
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Then verify Room schemas stay clean:

```bash
git status --porcelain -- app/schemas
```

Expected result: no output. If `app/schemas` changes, inspect the generated JSON and commit it only when the database schema intentionally changed.

## Release From x86-64

Preferred release path remains GitHub Actions:

```bash
gh auth login
gh workflow run release.yml \
  --ref main \
  -f version_name=X.Y.Z \
  -f version_code=N \
  -f release_notes="..."
```

Use local `assembleRelease` only when signing credentials are available on the x86-64 server and the security implications are acceptable.

## Post-Migration Cleanup

After confirming the x86-64 server can build and test:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
git status --short
```

Keep `AGENTS.md` updated if the old `aarch64` limitation no longer applies to the active development environment. If both machines remain in use, keep the architecture-specific notes and state which machine is authoritative for local Gradle verification.
