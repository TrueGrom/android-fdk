# Android Fast Development Kit

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

## Requirements

- Android **minSdk 26** 
- Java / Kotlin JVM target **17**

## License

[Apache License 2.0](LICENSE).
