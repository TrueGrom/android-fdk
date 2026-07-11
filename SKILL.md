---
name: using-fdkit
description: >
  Guide for building Android app features with FdKit (Android Fast Development Kit,
  io.github.truegrom:* artifacts). Use when writing or reviewing code that consumes FdKit —
  above all its state-management core (StateViewModel + builder traits, RemoteData,
  one-shot actions and error reactions), plus screens built on
  FdKitBaseScaffold/ViewModelScreen, BaseRepository data layers, the shared Ktor client,
  CryptoManager, FdkLog, and the datetime formatters.
---

# Using FdKit — Android Fast Development Kit

FdKit is a modular Android SDK (group `io.github.truegrom`, Kotlin packages `grmv.android.fdk.*`)
that provides a complete vertical slice for feature development: typed error handling,
cancellation-safe `Result` operators, a builder-based `StateViewModel`, Hilt-wired networking and
crypto, and slot-based Jetpack Compose screen scaffolding. Requirements: **minSdk 26**,
**JVM target 17**, Hilt, Compose (Material3).

## 1. Installation

Artifacts live on Maven Central. Always prefer the BOM so module versions stay aligned:

```kotlin
dependencies {
    implementation(platform("io.github.truegrom:bom:<version>"))

    implementation("io.github.truegrom:utils")       // Result/Either/Value + coroutine helpers
    implementation("io.github.truegrom:state")       // StateViewModel, RemoteData, traits, errors
    implementation("io.github.truegrom:viewmodel")   // BaseViewModel (task/uniqueTask)
    implementation("io.github.truegrom:repository")  // BaseRepository + BaseDispatchers
    implementation("io.github.truegrom:logging")     // FdkLog / LogSink / Timber backend
    implementation("io.github.truegrom:http-error")  // HttpError hierarchy + HttpErrorMapper
    implementation("io.github.truegrom:network")     // pre-configured Ktor HttpClient (@BaseHttp)
    implementation("io.github.truegrom:crypto")      // CryptoManager (Tink + Android Keystore)
    implementation("io.github.truegrom:datetime")    // java.time format extensions
    implementation("io.github.truegrom:ui-kit")      // Compose primitives + localized formatters
    implementation("io.github.truegrom:screen")      // FdKit* screen scaffolding
}
```

Resolve `<version>` to the latest release — check the Maven Central badge in the project README or
`https://central.sonatype.com/artifact/io.github.truegrom/bom`; never guess a version.

Snapshots: add `maven("https://central.sonatype.com/repository/maven-snapshots/")` and pin
`bom:<version>-SNAPSHOT`. Never ship snapshots in production.

Module dependency direction (low → high): `utils` → `logging`/`http-error` → `viewmodel`/`repository`
→ `state` → `ui-kit` → `screen`. Pull in only what the layer needs.

## 2. One-time app setup

### Logging

`FdkLog` is a no-op until a backend is installed. In `Application.onCreate`:

```kotlin
if (BuildConfig.DEBUG) FdkLogging.setupDebug()   // plants a Timber DebugTree once; guard repeat calls
// or: FdkLog.install(myCustomLogSink)           // e.g. Crashlytics-backed LogSink for release
```

Per-class loggers: `logger<MyClass>()` (reified) or `loggerForClass()` (runtime class — use in base
classes). `BaseViewModel` and `BaseRepository` already expose a `logger` property.

### Mandatory Hilt binding — base URL

The `network` module fails the DI graph build unless the app binds `HttpConfigProvider`:

```kotlin
@Module @InstallIn(SingletonComponent::class)
interface AppHttpModule {
    @Binds fun bindHttpConfig(impl: AppHttpConfig): HttpConfigProvider
}

class AppHttpConfig @Inject constructor() : HttpConfigProvider {
    override fun getBaseUrl() = "https://api.example.com/"   // keep the trailing slash
}
```

### Optional Hilt bindings — tuning

`HttpTimeoutConfig`, `HttpJsonConfig`, and `CryptoConfig` are optional (`@BindsOptionalOf` inside
the SDK). Bind an implementation only to override defaults; otherwise SDK defaults apply
(timeouts 30/20/35 s, `expectSuccess = true`, `followRedirects = false`; JSON
`ignoreUnknownKeys = true`, `explicitNulls = false`; crypto AES256-GCM keyset under
`tink_prefs`). Changing `CryptoConfig.keysetName`/`prefFileName`/`masterKeyUri` makes previously
encrypted data undecryptable.

### Compose defaults

Wire app-wide screen theming once, just inside the app theme:

```kotlin
AppTheme {
    FdkScreenDefaults(
        // each param defaults to the current Local*Defaults; override any subset:
        pagingDefaults = AppPagingDefaults,          // loaders for PagingContent
        errorEffectsDefaults = AppErrorDefaults,     // Throwable -> ErrorMessage mapping + dialog
        // contentPaddingDefaults, loadingDefaults, topBarDefaults, refreshDefaults, baseScaffoldDefaults ...
    ) {
        AppContent()
    }
}
```

## 3. Core utilities (`utils`)

**Result helpers** (`grmv.android.fdk.utils`): `value.success()`, `throwable.failure<T>()`,
`nullable.successOrFailureIfNull()` (null → `NullPointerException` failure), alias
`SomeResult = Result<Unit>`.

**Cancellation-safe operators** (`grmv.android.fdk.coroutines`) — the rule that matters most in
coroutine code:

- `runCatchingRethrowCancellation { ... }` — drop-in `runCatching` that rethrows
  `CancellationException` instead of capturing it. Prefer it over `runCatching` in any suspend path.
- `result.onError { e -> ... }` — like `onFailure` but rethrows cancellation *eagerly*, before the
  block runs. Because it escapes the chain, a chained "finally-like" operator only fires when placed
  first; for cleanup that must run even on cancellation, wrap the whole chain in `try/finally` —
  do not invent a `Result`-extension "finally" operator.
- `result.onAnyResult { ... }` — side effect on success and non-cancellation failure.
- `throwable.rethrowCancellation()` — call at the top of any `catch (e: Throwable)`.
- `dispatcher.context { ... }` — readable `withContext` shorthand.

**Either** — `Either.Left` (error) / `Either.Right` (success) with `fold`, `getOrElse`,
`onLeft`/`onRight`, `isLeft()`/`isRight()` (smart-casting contracts), `toLeft()`/`toRight()`, and
`runCatchingEither(factory) { ... }` (catches everything incl. cancellation — in coroutines prefer
`runCatchingRethrowCancellation`). This is the return type of `BaseRepository.httpSafeCall`.

**Value** — `Some`/`None` optional for "absent, no error context": `Value.noneIfNull(x)`,
`result.someOrNone()`.

## 4. Data layer (`repository`, `http-error`, `network`)

Bind an `HttpErrorMapper` (a `fun interface`) translating your HTTP client's exceptions into the
typed hierarchy, then extend `BaseRepository`:

```kotlin
class UserRepository @Inject constructor(
    dispatchers: BaseDispatchers,          // Hilt-provided; inject a test dispatcher in unit tests
    errorMapper: HttpErrorMapper,
    @BaseHttp private val client: HttpClient,   // SDK's pre-configured Ktor client
) : BaseRepository(dispatchers, errorMapper) {

    suspend fun profile(): Either<HttpError, Profile> = ioContext {
        httpSafeCall { client.get("users/me").body<Profile>() }
    }
}
```

Rules:

- `httpSafeCall { }` returns `Either<HttpError, T>` and never throws (cancellation excepted);
  `http { }` returns `T` and throws the mapped `HttpError` — use it when an upstream error sink
  (ViewModel) handles failures.
- Neither switches dispatchers — wrap blocking/non-main-safe work in `ioContext { }`.
- Branch on `HttpError` subtypes: `ResponseError(code)` (non-2xx), `NetworkError` (transient —
  offer retry), `ContentError` (deserialization — contract bug, don't retry), `UnknownError`.
- The `@BaseHttp` `HttpClient` already has ContentNegotiation(JSON), retry plugin, base URL, and
  timeout config; inject it rather than constructing clients.

## 5. State management (`viewmodel`, `state`) — the core of FdKit

This is FdKit's central pattern; get it right first. The model separates four channels, each with
its own type, ownership rule, and UI consumer:

| Channel | Producer type | Read-only contract for UI | UI consumer |
|---|---|---|---|
| Persistent screen state | `StateViewModel` / `MutableStateOwner<T>` | `StateOwner<T>` (`StateFlow<T>`) | `collectAsStateWithLifecycle` / `Fetchable` |
| Remote-request lifecycle | `RemoteData<T>` inside state | (part of state) | `Fetchable` slots |
| One-shot actions (navigation, snackbars) | `MutableActionManager<T>` | `ActionEmitter<T>` | `EventEffects` / `ConsumeEvents` |
| Error presentations | `MutableErrorManager<A>` | `ErrorEmitter<A>` | `ErrorEffects` |
| Pull-to-refresh in-flight flag | `RefreshController` | `RefreshOwner` (`StateFlow<Boolean>`) | `FdKitRefresh*` containers |

Everything the screen *is* lives in one immutable `BaseState`; everything that *happens once*
(navigate, toast) goes through actions/errors and is consumed, never stored in state.

### BaseViewModel — coroutine discipline

Launch all background work through the helpers — never `viewModelScope.launch` directly:

- `task { }` / `asyncTask { }` — fire-and-forget / awaitable, cancelled with the ViewModel.
- `uniqueTask(id) { }` / `asyncUniqueTask(id) { }` — keyed; a new run cancels the previous job
  with the same id (typed-ahead search, debounced saves). For pull-to-refresh prefer the
  `RefreshController` mixin (below), which coalesces repeated pulls instead of restarting them.
- `result handledError { e -> ... }` — logs the failure via the built-in `logger`, then runs the block.

### Defining state, builder, and ViewModel

State is an immutable `data class` implementing `BaseState` (directly or via trait markers).
Mutations go exclusively through a per-call builder — direct assignment to state is impossible
by construction:

```kotlin
data class ProfileState(
    override val remoteData: RemoteData<Profile> = RemoteData.loading(),
    override val locked: Boolean = false,
    val draftName: String = "",
) : RemoteDataState<ProfileState, Profile>, LockableState<ProfileState> {
    override fun withRemoteData(remoteData: RemoteData<Profile>) = copy(remoteData = remoteData)
    override fun withLocked(locked: Boolean) = copy(locked = locked)
}

class ProfileStateBuilder(override val initial: ProfileState) :
    BaseStateBuilder<ProfileState>(),
    RemoteDataOps<ProfileState, Profile>,   // adds loading()/fetched(data)/failed(error)
    LockOps<ProfileState> {                 // adds lock()/unlock()

    fun draftName(name: String) = accumulate { it.copy(draftName = name) }   // custom mutation
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserRepository,
) : StateViewModel<ProfileState, ProfileStateBuilder>(
    MutableStateOwner { ProfileState() }, ::ProfileStateBuilder,
), ErrorManager<Nothing> by MutableErrorManager() {

    init { refresh() }

    fun refresh() = uniqueTask("refresh") {          // re-trigger cancels the in-flight run
        state { loading() }
        repository.profile()
            .onRight { profile -> state { fetched(profile) } }
            .onLeft { error -> state { failed(error) } }
    }

    fun save() = task {
        state { lock() }                             // one emission: UI disables inputs
        repository.save(getActualState().draftName)
            .onLeft { error -> showError(error) { snackbar() } }
        state { unlock() }
    }
}
```

### How `state { }` works — atomicity guarantees

`state { ... }` creates a **fresh builder** seeded with the current snapshot via the builder
factory, runs your block against it, and emits the result through an atomic
`MutableStateFlow.updateAndGet`. Consequences:

- Multiple mutations inside one block — trait calls, custom methods, raw `accumulate` — fold in
  call order into **one** emission. Batch related mutations; never emit intermediate states the UI
  shouldn't see (e.g. `lock()` + `loading()` belong in one block).
- `accumulate { prev -> next }` defers the transform; `build()` folds all transforms over
  `initial`, each seeing the previous result. Read fields off the lambda parameter, not from a
  stale outer snapshot.
- Builders are intentionally **not thread-safe** — each is confined to one `state` call. Never
  cache or share a builder.
- `state { }` returns the new state; `getActualState()` gives a suspend-free read for logic.
  The UI must collect the `state` Flow instead.

### Extending the mutation surface: traits vs delegates

Two composition mechanisms, chosen by what the concern *is*:

**Trait** — a reusable *set of mutations* over a state shape (loading flags, pagination cursors in
state, lockability). Pair an F-bounded state marker with an ops interface whose default methods
call `accumulate`; any builder gains the vocabulary by adding a supertype:

```kotlin
interface LoadableState<S : LoadableState<S>> : BaseState {
    val loading: Boolean
    fun withLoading(loading: Boolean): S
}

interface LoadingOps<S : LoadableState<S>> : BuilderOps<S> {
    fun showLoading() = accumulate { it.withLoading(true) }
    fun hideLoading() = accumulate { it.withLoading(false) }
}
```

Traits compose freely on one builder, and all become available inside a single `state` block —
still one atomic emission. Built-ins: `RemoteDataOps` (drives a `RemoteData` payload) and
`LockOps` (UI interaction lock). Limitation: a state can implement `RemoteDataState` only once
(Kotlin forbids repeating a generic supertype) — for several remote payloads, hand-roll per-holder
methods (`fun userLoading() = accumulate { ... }`, etc.).

**Delegate** — a *stateful collaborator* with its own fields, coroutines, and lifecycle (a
paginator holding a cursor, a websocket controller, a background sync worker). Extend
`StateViewModelDelegate<T, B>`; it launches via the owner-scoped `task { }` and mutates through
the owner's builder with the same `state { }` API:

```kotlin
class ProfileLoaderDelegate(
    private val fetch: suspend () -> Profile,
) : StateViewModelDelegate<ProfileState, ProfileStateBuilder>() {

    fun refresh() = task {
        state { loading() }
        runCatchingRethrowCancellation { fetch() }
            .onSuccess { profile -> state { fetched(profile) } }
            .onError { error -> state { failed(error) } }
    }
}

class ProfileViewModel(/* ... */) : StateViewModel<ProfileState, ProfileStateBuilder>(...) {
    val loader = ProfileLoaderDelegate(fetch = ::fetchProfile).also { it.initialize(this) }
}
```

`initialize(owner)` must be called exactly once before any other delegate method — every call
before it throws `UninitializedPropertyAccessException`. Rule of thumb: new *verbs* over existing
state → trait; new *owned state or coroutines* → delegate.

### Pull-to-refresh: RefreshOwner + RefreshController

Pull-to-refresh has its own dedicated channel — the in-flight flag lives *outside* the screen
state, in a `RefreshOwner` (`refreshing: StateFlow<Boolean>` + `refresh()`), so the UI container
recomposes only on flag changes. Mix it into any ViewModel (not just `StateViewModel`) by class
delegation and bind scope + work in `init`:

```kotlin
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val refresher: RefreshController = RefreshController(),
) : StateViewModel<FeedState, FeedStateBuilder>(...), RefreshOwner by refresher {

    init { refresher.initialize(viewModelScope, ::reload) }

    private suspend fun reload() { /* same code path as the initial load */ }
}
```

Guarantees baked into `RefreshController` — do not re-implement them:

- Concurrent pulls **coalesce** into one run (atomic `compareAndSet` gate).
- The flag resets in `finally` — recovers on failure *and* cancellation.
- **No error handling**: failures inside the work must flow through the standard error path
  (`ErrorManager` / `visualError`), same as any other operation.
- `refresh()` before `initialize()` throws `UninitializedPropertyAccessException` (fail-fast).

Two-phase `initialize()` is deliberate: the `by`-delegation expression cannot reference
`viewModelScope`. Screens whose state already carries a refreshing flag skip the mixin and use the
primitive `FdKitRefresh*` overloads (section 6) — the approaches interoperate without adapters.

### RemoteData — the request-lifecycle value

`RemoteData<T>` is a sealed class: `Loading`, `Fetched(data)`, `Error(error)` — exhaustive `when`
without an `else`. Construct via `RemoteData.loading()/fetched(x)/error(e)`. Helpers
(`grmv.android.fdk.state`):

- `isFetched()` — smart-casting check; `ifFetched { data -> ... }` — conditional side effect.
- `fetchedOrDefault { fallback }` — safe extraction.
- `unwrap()` / `mustBeFetched()` — throwing casts; only where non-fetched is a programming error.
- `contentKey` — stable per-variant string for Compose `key(...)`/animated transitions keyed on
  the phase rather than the payload.

Keep `RemoteData` *inside* the state (via `RemoteDataState`) rather than exposing it as a separate
flow — the UI then renders it with `Fetchable` (section 6) off the single state stream.

### One-shot actions and errors

- **Actions** (navigation, custom events): implement `ActionEmitter<T>` by delegating to
  `MutableActionManager<T>()`; emit with `sendAction(event)`. The flow is conflated — a new action
  replaces an unconsumed one; the UI consumer (`EventEffects` / `ConsumeEvents`) dispatches and
  calls `consumeAction`, which resets to `null` only if that action is still current. Model events
  as a sealed interface; mark snackbar-worthy ones with `SnackbarEvent`.
- **Errors**: implement `ErrorManager<Action>` via `MutableErrorManager()` (alias
  `Errors = ErrorManager<Nothing>` when no follow-up action). Choose presentation at the emit
  site with the builder DSL — `toast()`, `snackbar()`, `dialog()`, optionally
  `.withAction(SomeAction)` — producing an `ErrorReaction` the UI renders and consumes. The infix
  `visualError` plugs straight into `Result` chains (and rethrows cancellation, like `onError`):

```kotlin
repository.save(data) visualError { snackbar() }
repository.save(data) visualError { dialog().withAction(RetryAction) }
```

### Exposure discipline

Expose only read-only contracts to the UI layer: `StateOwner<T>`, `ActionEmitter<T>`,
`ErrorEmitter<A>`. The mutable counterparts (`MutableStateOwner`, `MutableActionManager`,
`MutableErrorManager`, the `state { }` builder) stay inside the ViewModel. `BaseState` and
`RemoteData` are `@Immutable` — keep every field deeply immutable so Compose skipping works.

### Testing state logic

Inject the pieces instead of hardcoding them: pass a seeded `MutableStateOwner { FixtureState() }`
into the `StateViewModel` constructor, a test dispatcher through `BaseDispatchers` at the
repository seam, and assert on `viewModel.state.value` transitions after each intent. Builders are
plain objects — `FooStateBuilder(initial).apply { mutation() }.build()` unit-tests a mutation
without any ViewModel.

## 6. Screens (`screen`, `ui-kit`)

Screen anatomy — every FdKit screen follows this shape:

```kotlin
@Composable
fun ProfileScreen(onBack: () -> Unit) = ViewModelScreen<ProfileViewModel> {  // Hilt-provided VM
    val snackbar = rememberSnackbarManager()

    ErrorEffects(snackbarHostState = snackbar.hostState)   // renders ErrorReaction dialog/snackbar/toast
    snackbar.ConsumeEvents(viewModel)                      // shows SnackbarEvent actions
    EventEffects<_, ProfileEvent> { event ->               // remaining custom actions
        when (event) { ProfileEvent.Close -> onBack() }
    }

    FdKitBaseScaffold(
        topBar = { FdKitTopBarTextTitle(title = "Profile", onBack = onBack) },
        snackbarHost = { ScreenSnackbarHost(snackbar) },
    ) {
        viewModel.Fetchable<Profile, ProfileState> {
            retry { viewModel.refresh() }                  // default error UI + retry wiring
            Fetched { profile ->
                FdKitScreenColumn {                        // static body; FdKitScrollableScreen / FdKitLazyScreen otherwise
                    Text(profile.name)
                }
            }
        }
    }
}
```

Building blocks:

- **`ViewModelScreen<VM> { }`** creates a `ViewModelScope` (retrieves the VM from Hilt) — the
  receiver for `ErrorEffects`/`EventEffects` and `FdKitBaseScaffold`.
- **`FdKitBaseScaffold`** — Material3 Scaffold wrapper. Slots (`topBar`, `bottomBar`,
  `snackbarHost`, `floatingActionButton`) receive `ScaffoldSettings` exposing the shared
  `scrollBehavior` so `FdKit*TopBar` presets collapse with content. The body receives a
  `ScaffoldScope` on which the content helpers are callable.
- **Content helpers** (on `ScaffoldScope`): `FdKitScreenColumn` (static), `FdKitScrollableScreen`
  (eager scroll column), `FdKitLazyScreen` (LazyColumn). All apply `ContentPaddingDefaults`.
- **Pull-to-refresh containers**: `FdKitRefreshContainer` (Box, optionally self-scrolling),
  `FdKitRefreshColumn` (eager scroll column), `FdKitRefreshLazyColumn` (LazyColumn) — Material3
  `PullToRefreshBox` wrappers whose indicator comes from `RefreshDefaults`
  (`LocalRefreshDefaults`, settable via `FdkScreenDefaults`). Each comes in two overloads: a
  `RefreshOwner`-receiver one that collects the flag lifecycle-aware and triggers `refresh()`
  (`viewModel.FdKitRefreshLazyColumn { items(...) { ... } }`), and a primitive one
  (`isRefreshing` + `onRefresh`) for screens that manage the flag themselves.
- **`Fetchable`** — renders `RemoteData` with slot DSL: `Fetched { }`, optional `Loading { }`,
  `Error { }` or `retry { }` (`retry` keeps the app-wide error UI and wires the callback; last
  writer wins between `Error`/`retry`). The `StateViewModel` overload collects state
  lifecycle-aware; the plain `RemoteData<T>.Fetchable` overload serves nested fields.
- **`PagingContent`** — `Flow<PagingData<T>>.PagingContent(itemKey = { it.id }) { Item { i, x -> ... } }`.
  Pull-to-refresh built in; slots `Loading/Error/Empty/AppendLoading/AppendError/Prepend` fall back
  to `LocalPagingDefaults`. Programmatic refresh: `rememberPagingController()` passed as
  `controller`, then `controller.refresh()`/`retry()`.
- **Snackbars are UI-only**: ViewModels never hold a `SnackbarManager`; they emit events whose type
  implements `SnackbarEvent` (declares its snackbar via the `SnackbarBuilder` DSL).
  `snackbar.ConsumeEvents(viewModel)` consumes only `SnackbarEvent`s; everything else stays pending
  for `EventEffects` — the two compose safely on one screen.
- **Top bars**: `FdKitTopBarTextTitle` / `FdKitTopBarHeadlineTitle` (back-arrow defaults),
  `FdKitFeatureTopBar` (no back default — avatar/menu). All read `LocalTopBarDefaults` and wire
  `scrollBehavior` from `ScaffoldSettings`.
- **ui-kit**: `FdKitCenterBox` (Box- and Column-scoped centering), `currentLocale`, and
  locale-aware date formatting in composables:
  `localizedFormat(date) { ddMMMyyyy() }` — recomposes on device-language change.

## 7. Crypto (`crypto`)

Inject the singleton `CryptoManager` directly (no factory). AES-256-GCM via Tink, keyset wrapped
by a hardware-backed Keystore master key — ciphertext is device-bound and lost with app-data clear.

```kotlin
cryptoManager.encryptString(token, aad = "refresh_token".toByteArray())  // Result<String>, Base64
cryptoManager.decryptString(cipher, aad = "refresh_token".toByteArray()) // must pass identical aad
```

AAD rules: it is authenticated-but-not-encrypted context — **not a salt, not secret**; never put
confidential data in it. Use per-call `aad` to bind ciphertext to a purpose; omitted `aad` falls
back to `CryptoConfig.defaultAad`, then device `ANDROID_ID`, then empty. All methods return
`Result` — handle failure (Keystore unavailable, AAD mismatch, malformed input).

## 8. Datetime (`datetime`)

Extension functions on `LocalDate`/`LocalDateTime`, all taking an explicit `Locale`
(named after their pattern): `ddMMyyyy`, `ddMMM`, `ddMMMyyyy`, `MMMdyyyy`, `EEEE`, `MMMMyyyy`,
`Hmm`, `ddMMyyyyHmm`, plus `*OptionalYear` variants that omit the year when it equals the current
year (system clock at call time) and `rangeOptionalYear(locale, start, end)`. In Compose, prefer
the `ui-kit` `localizedFormat` wrappers, which resolve the ambient locale.

## 9. Rules checklist

When writing consumer code, enforce:

1. Never swallow `CancellationException` — use `runCatchingRethrowCancellation`/`onError`, and
   plain `try/finally` for must-run cleanup.
2. All ViewModel coroutines go through `task`/`uniqueTask`; keyed variants for supersede-previous
   semantics.
3. State mutations only via `state { }` builder blocks; batch related mutations into one block.
4. Repositories return `Either<HttpError, T>` (or throw typed `HttpError` via `http`); wrap
   blocking calls in `ioContext`.
5. UI observes: `Fetchable` for `RemoteData`, `ErrorEffects` for `ErrorReaction`, `EventEffects` /
   `ConsumeEvents` for one-shot actions, `FdKitRefresh*` containers for `RefreshOwner` — each
   consumer marks its events consumed.
6. Expose read-only contracts to the UI (`StateOwner`, `ErrorEmitter`, `ActionEmitter`); keep the
   mutable managers internal to the ViewModel.
7. Theme app-wide via `FdkScreenDefaults` once; per-call slot parameters override locally.
8. Public FdKit types are `Fdk`/`FdKit`-prefixed; follow the same convention when extending the SDK.
