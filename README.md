# Android Fast Development Kit

[![Maven Central](https://img.shields.io/maven-central/v/io.github.truegrom/bom?label=release)](https://central.sonatype.com/artifact/io.github.truegrom/bom)
![Snapshot](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fio%2Fgithub%2Ftruegrom%2Fbom%2Fmaven-metadata.xml&label=snapshot)

**FdKit** — Android **F**ast **D**evelopment **K**it. A modular Android library providing
state, networking, persistence, logging, and slot-based Compose UI primitives for building apps fast.

- **Group:** `io.github.truegrom`
- **Java / Kotlin JVM target:** 17
- **minSdk:** 26

## Modules

All modules share the group `io.github.truegrom`; the artifactId is the module name.

| Coordinate                    | Purpose                                                     |
|-------------------------------|-------------------------------------------------------------|
| `io.github.truegrom:utils`        | Core utilities + coroutines                                 |
| `io.github.truegrom:state`        | Compose state primitives                                    |
| `io.github.truegrom:repository`   | Base repository + error mapping                             |
| `io.github.truegrom:viewmodel`    | Base ViewModel / lifecycle helpers                          |
| `io.github.truegrom:logging`      | Logger abstraction over Timber                              |
| `io.github.truegrom:http-error`   | HTTP error types / mapping                                  |
| `io.github.truegrom:crypto`       | `CryptoManager` and crypto helpers (Tink)                   |
| `io.github.truegrom:network`      | Ktor client / network primitives                            |
| `io.github.truegrom:datetime`     | `java.time` parse/format helpers                            |
| `io.github.truegrom:ui-kit`       | Jetpack Compose UI kit                                      |
| `io.github.truegrom:screen`       | Slot-based Compose screen scaffolding (`FdKit*` components) |
| `io.github.truegrom:bom`          | Bill of Materials — pins compatible versions for all above  |

## Installation

Artifacts are published to **Maven Central**. Ensure `mavenCentral()` is in your repositories:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

### Using the BOM (recommended)

The **BOM** (Bill of Materials) pins a mutually-compatible version for every FDK module.
Declare the BOM version once via `platform(...)`, then depend on modules **without** a version:

```kotlin
dependencies {
    implementation(platform("io.github.truegrom:bom:<version>"))

    // versions are supplied by the BOM
    implementation("io.github.truegrom:crypto")
    implementation("io.github.truegrom:screen")
    // …add other modules as needed
}
```

Upgrading the SDK is then a single change: bump the BOM version. The BOM itself contains no
code — it only aligns module versions; each module's artifact is still fetched from Maven Central.

### Without the BOM

Pin each module version explicitly:

```kotlin
dependencies {
    implementation("io.github.truegrom:crypto:<version>")
    implementation("io.github.truegrom:screen:<version>")
}
```

### Snapshots

Every push to `master` publishes a `-SNAPSHOT` build to the Central Portal snapshot repository
(the current version is shown in the *snapshot* badge above). To use it, add the snapshot
repository and pin the snapshot version:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }
}
```

```kotlin
dependencies {
    implementation(platform("io.github.truegrom:bom:<version>-SNAPSHOT"))
}
```

Snapshots are mutable — the same `-SNAPSHOT` version resolves to the newest build on each
dependency refresh. Use them for trying unreleased changes, not in production.

## AI coding agents

[`SKILL.md`](SKILL.md) is a machine-readable guide that teaches AI coding agents how to build
features with FdKit — state management, screens, data layer, and the setup bindings. It follows
the [Agent Skills](https://agentskills.io) format (plain Markdown with YAML frontmatter), so any
agent can consume it. Wire it into your app's repo according to your tool:

- **Claude Code**: copy to `.claude/skills/using-fdkit/SKILL.md` — picked up automatically for
  FdKit-related work.
- **Cursor / Copilot / other agents**: add it as a rule or instructions file (e.g.
  `.cursor/rules/`, `.github/copilot-instructions.md`), or reference it as context in prompts.

```bash
mkdir -p .claude/skills/using-fdkit
curl -o .claude/skills/using-fdkit/SKILL.md \
  https://raw.githubusercontent.com/TrueGrom/android-fdk/master/SKILL.md
```

## Requirements

Your app must meet these to build against FdKit:

| Requirement             | Value       | Notes                                              |
|-------------------------|-------------|----------------------------------------------------|
| `minSdk`                | **26+**     | modules are built with minSdk 26                   |
| `compileSdk`            | **37+**     | AAR metadata declares `minCompileSdk = 37`         |
| Core library desugaring | **enabled** | modules are built with desugaring enabled          |
| Java / Kotlin target    | **17+**     | modules ship Java 17 bytecode                      |
| Kotlin                  | **2.2+**    | modules are compiled with Kotlin 2.3               |

Module-specific:
- `crypto`, `network`, `repository`, `viewmodel`, `screen` provide their bindings via **Hilt**.

## License

[Apache License 2.0](LICENSE).
