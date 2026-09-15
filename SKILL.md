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
crypto, and slot-based Jetpack Compose screen scaffolding. Your app must be on **minSdk 26** or
higher, compile against **JVM target 17**, and use Hilt and Compose (Material3).

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

Resolve `<version>` to the latest release listed at
`https://central.sonatype.com/artifact/io.github.truegrom/bom`; never guess a version. Snapshot
builds are not intended for consumers.

Artifacts bring their own dependencies transitively — declare only the ones whose API you use
directly (`screen`, for instance, already pulls in `state` and `ui-kit`).

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

`HttpTimeoutConfig`, `HttpJsonConfig`, `HttpRetryConfig`, and `CryptoConfig` are optional. Bind an
implementation only to override defaults; without a binding the SDK defaults apply
(timeouts 30/20/35 s, `expectSuccess = true`, `followRedirects = false`; JSON
`ignoreUnknownKeys = true`, `explicitNulls = false`, `coerceInputValues = true`; **no automatic
retries**; crypto AES256-GCM keyset under `tink_prefs`). Changing
`CryptoConfig.keysetName`/`prefFileName`/`masterKeyUri` makes previously encrypted data
undecryptable.

**Retries are opt-in.** `HttpRetryConfig.maxRetries` defaults to `0`, which skips installing
`HttpRequestRetry` altogether — a failed request surfaces on its first attempt. Turn it on
deliberately, because the backoff is paid in screen time: three attempts sleep ~1 s, 2 s and 4 s
plus jitter, so a block rendering a loading state holds it ~7-10 s longer (more against a
`Retry-After`). Only `GET`/`HEAD`/`OPTIONS` are replayed unless `retryableMethods` widens it.

`shouldRetry(HttpRetryContext)` narrows retries the app has enabled — it is consulted after the
`maxRetries`/`retryableMethods` checks, and returning `false` fails that request fast without
disabling retries client-wide. The context carries `method`, `url`, `status` (5xx response) or
`cause` (transport `IOException`, exactly one of the two), and `retry` counting from 1. The
canonical use is answering from `ConnectivityManager`: with no transport at all, backoff cannot
help, so do not spend it.

```kotlin
class AppRetryConfig @Inject constructor(
    private val connectivity: ConnectivityObserver,
) : HttpRetryConfig {
    override val maxRetries = 3
    override val retryableMethods = setOf(HttpMethod.Get, HttpMethod.Head)
    override fun shouldRetry(context: HttpRetryContext) = connectivity.isOnline
}
```

**JSON coercion** (`coerceInputValues = true`) keeps a value the client cannot represent from
failing the whole body: an unknown enum constant decodes to the property's default, or to `null`
when the property is nullable (`explicitNulls = false` makes that work). It applies per *class
property* only — an unknown constant inside a `List<SomeEnum>` still fails the entire response, so
model enum-typed list elements as raw `String` and parse them in the repository. Its price is
silence: an incoming `null` for a non-nullable property with a default lands on that default
instead of failing loudly.

### Compose defaults

Wire app-wide screen theming once, just inside the app theme:

```kotlin
AppTheme {
    FdkScreenDefaults(
        // each param defaults to the current Local*Defaults; override any subset:
        pagingDefaults = AppPagingDefaults,          // loaders for paged lists AND grids
        errorEffectsDefaults = AppErrorDefaults,     // Throwable -> ErrorMessage mapping + dialog
        contentTransitions = FdkFadeContentTransitions, // opt in to animated state swaps
        // contentPaddingDefaults, loadingDefaults, topBarDefaults, refreshDefaults,
        // baseScaffoldDefaults ...
    ) {
        AppContent()
    }
}
```

Scaffold slot defaults are overridden the same way, `copy` keeping the rest — e.g. to opt the whole
app out of the keyboard padding that `FdKitBaseScaffold` applies by default:

```kotlin
baseScaffoldDefaults = LocalFdKitBaseScaffoldDefaults.current.copy(avoidKeyboard = false),
```

**State-slot animations are opt-in.** `LocalContentTransitions` defaults to
`FdkNoContentTransitions`, so `Fetchable` and `PagingContent` swap slots in a single frame, without
animation. `FdkFadeContentTransitions` cross-fades both.

Implement `FdkContentTransitions` to supply your own specs. `transform()` returns a
`ContentTransform?` (as for `AnimatedContent`) and drives `Fetchable`; `itemTransitions()` returns an
`FdkItemTransitions?` — `fadeIn`/`fadeOut`/`placement` specs fed to `Modifier.animateItem` — and
drives `PagingContent` and the paged grids. `null` from either means "no animation". Install it
app-wide through `FdkScreenDefaults(contentTransitions = ...)`, or scope a subtree with
`ProvideContentTransitions(...)`. A per-call `transition { }` runs inside each emitted slot, so
whatever it `remember`s lives and dies with that slot.

Two behaviours worth knowing. `Fetchable` does not animate container size: while both slots are
present the container is sized to their union, so a large slot fading out holds it open until the
fade ends — visible only where both slots wrap their content, since two `fillMaxSize` slots make the
union the viewport and the container never resizes.

`PagingContent` animates its load-state slots only, never the loaded items, so a paged list's first
render is unchanged: the initial loader is there from the list's first frame (which a lazy layout
does not animate in) and is then removed instantly so it never covers the arriving list. The
animation shows on the append path — next-page footer in and out, items above it settling — and on
the error/empty slots. Items are left alone on purpose: a refresh replaces every key at once, so
animating them turns one swap into a cascade of per-item fades. Cross-fading a paged list as a whole
is not available: the refresh state lives inside `PagingContent`, so only a screen that already
tracks its own `RemoteData` beside the paging flow can wrap the list in a `Fetchable`.

While a `Fetchable` transition runs, its slots can read `LocalContentTransitionScope` (an
`AnimatedVisibilityScope?`) to animate their own children. Shared elements take it as a parameter;
`Modifier.animateEnterExit` is a member of `AnimatedVisibilityScope`, so it needs the scope as a
receiver — `with(scope) { Modifier.animateEnterExit(...) }`. The `SharedTransitionScope` comes from
your own `SharedTransitionLayout`. The value is `null` whenever the content is not animating —
with animation off, and always inside `PagingContent` — so branch on it rather than asserting.

While a transition runs both slots stay composed: the outgoing one keeps its effects running and
keeps accepting input until the exit animation ends. With the default fade that is a couple of
hundred milliseconds — long enough for a user to tap Retry in a fading-out `Error` slot a second
time and send a second request. Guard any slot action that is not idempotent; the transition will
not disable input for you.

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
  first; for cleanup that must run even on cancellation, wrap the whole chain in `try/finally`.
  There is no `Result`-extension "finally" operator.
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

    suspend fun profile(): Profile = ioContext {
        http { client.get("users/me").body() }   // throws mapped HttpError — pairs with the ViewModel Result recipes
    }
}
```

Rules:

- Prefer `http { }`: it returns `T` and throws the mapped `HttpError`, feeding the ViewModel
  `Result` recipes (section 5) directly. `httpSafeCall { }` returns `Either<HttpError, T>` and
  never throws (cancellation excepted) — for the rare call site that branches on `HttpError`
  subtypes in place.
- Neither switches dispatchers — wrap blocking/non-main-safe work in `ioContext { }`.
- Branch on `HttpError` subtypes: `ResponseError(code)` (non-2xx), `NetworkError` (transient —
  offer retry), `ContentError` (deserialization — contract bug, don't retry), `UnknownError`.
  The hierarchy is `sealed`, so a `when` over it is exhaustive and the compiler prevents a
  subtype of your own — carry app-specific context in `ResponseError.details` instead.
- When one status covers several situations (a `403` that means "link expired" vs. "link already
  spent"), parse the error body in the mapper and attach it as `ResponseError.details`
  (`FdkResponseErrorDetails`, nullable and defaulted — mappers that don't need it stay
  unchanged), then read it back with `detailsAs<T>()`. The SDK never inspects the value.
- Ready-made `FdkResponseErrorDetails` shapes — or implement the interface yourself:
  `FdkCodedError(code, detail)` (application error code), `FdkProblemDetails(...)` (RFC 9457
  `problem+json` — Spring Boot, ASP.NET Core), `FdkRawErrorBody(body, contentType)` (verbatim
  fallback; its `toString` hides the body). Requires http-error ≥ 0.3.0.
- Map API models to display-ready domain types in the repository — `LocalDate` (formatted via the
  datetime module), value-class ids — before results reach ViewModel state; state never holds raw
  DTO strings.
- The `@BaseHttp` `HttpClient` already has ContentNegotiation(JSON), retry plugin, base URL, and
  timeout config; inject it rather than constructing clients.

Carrying the error body across one status (`details` / `detailsAs`):

```kotlin
// mapper — the only place that has the body parsed
HttpError.ResponseError(code = 403, cause = e, details = FdkCodedError(body.errorCode))

// call site — the app names its own accessor
val HttpError.serverErrorCode: String? get() = detailsAs<FdkCodedError>()?.code
```

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
), ErrorManager<Nothing> by MutableErrorManager(),
   ActionManager<ProfileEvent> by MutableActionManager() {   // screen exposes only ActionEmitter

    init { load() }

    private suspend fun fetchData(): Result<Unit> = runCatchingRethrowCancellation {
        val profile = repository.profile()           // throwing repo method (built on http { })
        state { fetched(profile) }                   // success write INSIDE the catching block
    }

    fun load() = uniqueTask("load") {                // re-trigger cancels the in-flight run
        state { loading() }
        fetchData() handledError { state { failed(it) } }   // logs, then error → state → Fetchable
    }

    fun save() = task {
        state { lock() }                             // one emission: UI disables inputs
        try {
            runCatchingRethrowCancellation { repository.save(getActualState().draftName) }
                .visualError { snackbar() }
        } finally {
            state { unlock() }                       // runs on failure and cancellation too
        }
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

- **Actions** (navigation, custom events): implement `ActionManager<T>` (the writable interface, it
  extends the read-only `ActionEmitter<T>`) by delegating to `MutableActionManager<T>()`; emit with
  `sendAction(event)`, and expose only `ActionEmitter<T>`. The flow is conflated — a new action
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

### Standard recipes — initial load / refresh / user action

`ProfileViewModel` above already follows the standard shape: one shared `fetchData()` for every
entry point (built on a throwing `http { }` repository method — section 4), with entry points
differing only in spinner policy and error presentation. The third entry point — refresh — reuses
the same `fetchData()` under the same task key:

```kotlin
// Silent refresh (ON_RESUME, retry-after-action) — no loading(), transient error
fun refresh() = uniqueTask("load") {   // same key as load(): latest entry point wins
    fetchData() visualError { dialog() }
}
```

`visualError` shows the error but does **not** log it. When a production breadcrumb matters, chain
`handledError { }` (empty block — log-only) before it:
`fetchData() handledError { } visualError { dialog() }`.

Why the split — mixing the channels up is the classic mistake: a refresh failure must not wipe
good content into an error screen, and an initial-load failure must not be a dismissable
toast over a blank screen:

- **Load** errors are *persistent*: they replace the (absent) content, live in `remoteData`, and
  the screen renders the app-wide error UI with retry via `Fetchable`'s `retry { viewModel.load() }`
  slot.
- **Refresh** errors are *transient*: content is already on screen, so skip `loading()` (it would
  hide the list under the spinner) and surface the failure as a dialog/snackbar via `visualError` —
  not in state. For pull-to-refresh specifically, prefer the `RefreshController` mixin
  (`init { refresher.initialize(viewModelScope, ::reload) }` where
  `reload() = fetchData() visualError { dialog() }`) — it coalesces repeated pulls and resets the
  flag in `finally`; `uniqueTask` restarts instead.
- **Actions** never touch `remoteData`; they lock the UI (`LockOps`), report failure transiently,
  and clean up in `try/finally`: there is no `Result`-extension "finally" operator, because it
  could not run on cancellation — `onError`/`visualError` rethrow eagerly.
- All load-path entry points share one `uniqueTask` key so competing runs supersede each other.

Choosing an error operator:

| Situation | Operator |
|---|---|
| Load error → persistent error state with retry | `handledError { state { failed(it) } }` (logs, then block) |
| Transient error (refresh, action) → dialog/snackbar/toast | `visualError { dialog() }` |
| Side effect on failure, no logging | `onError { }` |
| Side effect on success *and* failure (not cancellation) | `onAnyResult { }` |
| Fire-and-forget failure (analytics, prefetch) | `handledError { }` — log only, never surface |
| Cleanup that must run even on cancellation | plain `try/finally` around the chain |
| Raw `Result.onFailure` / `fold` | avoid — both hand `CancellationException` to the lambda; use `onError` (plus `onSuccess` when both branches are needed) |

All of these rethrow `CancellationException` — never hand-roll `rethrowCancellation()` around them.
Paging screens are a separate case: `PagingContent`, `PagingGridContent` and `pagingItems` render
loading/error/empty themselves — no `RemoteData`, `Fetchable`, or `RefreshOwner` involved.

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
        topBar = { FdKitTopBarTextTitle(title = "Profile", onNavigateBack = onBack) },
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
  `ScaffoldScope` on which the content helpers are callable. `avoidKeyboard` (on by default) pads the
  **body** by the keyboard inset, so a form screen does not need `Modifier.imePadding()` — under
  `enableEdgeToEdge()` the window is never resized for the keyboard, and without it the submit button
  sits behind it with nothing to scroll. It shrinks the body's viewport rather than scrolling it, so
  it serves a **scrolling or bottom-anchored** body: put a form in `FdKitScrollableScreen`, since a
  static `FdKitScreenColumn` taller than what is left still overflows. Scope is the body alone — the
  `bottomBar` and the FAB stay behind the keyboard, `ScreenSnackbarHost` pads itself and rides above
  it regardless of the flag, and `ModalBottomSheet`/dialog content is a separate composition. Turn it
  off per screen (`avoidKeyboard = false`) or app-wide via `baseScaffoldDefaults` (see above). On
  API 26-29 the app manifest still needs `android:windowSoftInputMode="adjustResize"` for the inset
  to be reported at all; `adjustPan` breaks it on every API level. Neither is enforceable from the
  SDK.

  Two consequences worth knowing: a full-bleed layer inside the body (a background with
  `matchParentSize`) lays out into the shrunk box and so moves with the keyboard; and a FAB is never
  lifted over the keyboard — Material3 derives the snackbar's offset from the FAB's measured height,
  so padding that slot would count the inset twice. `hideFabWhenImeVisible` (off by default) drops
  the FAB from the composition instead while the keyboard is up — note that a snackbar visible at
  that moment drops by the FAB's height for one frame, since Material3 stacks it on top of the FAB.

  In a **debuggable** build the scaffold warns once through `FdkLog` if the host activity *declares*
  `windowSoftInputMode="adjustPan"`. It cannot see the case where an app declares nothing and the
  framework resolves the mode itself, so a silent app is not proof of a correct manifest. Release
  builds and apps without a `FdkLog` sink pay nothing.
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
  `transition { }` (returns `ContentTransform?`) overrides the state-swap animation for one call site
  — `transition { null }` opts out of an app-wide provider; unset, it follows
  `LocalContentTransitions`, which animates nothing by default. The transition is keyed on the
  `RemoteData` variant, so a new `Fetched` payload recomposes without re-running it.
- **`PagingContent`** — `Flow<PagingData<T>>.PagingContent(itemKey = { it.id.toString() }) { Item { i, x -> ... } }`
  (`itemKey` is `(T) -> String` — convert non-string ids). Pull-to-refresh built in; slots
  `Item/Loading/EmptyError/RefreshError/Empty/PrependLoading/PrependError/AppendLoading/AppendError/Header`, of which everything but
  `Item`, `Empty` and `Header` falls back to `LocalPagingDefaults` (`Empty` and `Header` have no
  default — render nothing unless you supply them). All three error slots receive the failing
  `Throwable` alongside `retry` (`EmptyError(e, retry)`, `RefreshError(e, retry)`, `AppendError(e, retry)`) — word it through
  `ErrorEffectsDefaults.errorMessage` — the mapper behind the error dialogs and snackbars — and word
  `LoadingDefaults.Error` from the same place, so one failure reads the same on a paged screen and
  on the screen beside it. `retry` stays `() -> Unit`: paging
  retries through `LazyPagingItems.retry()`. The value is re-read inside the slot's composition, so
  a second failure of a different kind is worded as itself. Programmatic refresh:
  `rememberPagingController()` passed as `controller`, then `controller.refresh()`/`retry()`.
  `rememberPagingController(showsRefreshIndicator = true)` makes `refresh()` raise the pull indicator
  for its duration — off by default, because a reload nobody asked for should not animate; turn it on
  for a toolbar button, which otherwise looks inert now that loaded items are never replaced. Hoist
  `state` (`LazyListState`, `LazyGridState` for the grid) to drive the scroll yourself.
  Pass `isRefreshing` + `onRefresh` to own the pull gesture when it reloads more than the paged
  content (a header, a summary): the indicator then follows your flag and `onRefresh` replaces the
  built-in `refresh()`, so reload the paging itself from it — via `controller`. Unset (the default),
  the container owns both. `transition { }` (returns `FdkItemTransitions?`) overrides the load-state slot animation for one
  call site — `transition { null }` opts out; unset, it follows `LocalContentTransitions`. Loaded
  items are never animated — only the load-state slots.
- **Loaded items are never replaced by a load-state slot.** `Loading` and `EmptyError` are the
  *empty-state* presentations: they render only while `itemCount == 0`. A `PagingSource` over a local
  store is invalidated by writes the screen never asked about, and every invalidation drives refresh
  back through `Loading` — branching on the load state alone would tear the content off the screen on
  each one. A refresh that *fails* over loaded items surfaces as the `RefreshError` banner instead,
  emitted first (after `Prepend`, full-span in a grid, wrapping its content) and defaulting to the
  append error's presentation, so an app that has skinned `AppendError` gets a matching banner
  without doing anything. Override `PagingDefaults.RefreshError` to tell the two apart. While the
  banner is up it is the only error surface — `AppendError` is suppressed, because `retry()` restarts
  every failed load state at once and two messages would report one outcome.
- **Migrating an existing `PagingDefaults`** (this release breaks it twice, deliberately): the slot
  receiver changes `LazyItemScope` -> `FdkPagingSlotScope`, and both error slots gain the failing
  throwable, the empty-state one is renamed, and the static-header DSL slot `Prepend` becomes
  `Header` (freed up for `PrependLoading`/`PrependError`, the real paging load states at that end,
  both of which default to their append twins) — `Error(retry)` -> `EmptyError(e, retry)`,
  `AppendError(e, retry)`. Both refresh slots fire off the same failed refresh and are told apart only
  by whether items are on screen, so the names say when they render, not what failed. Slot bodies are
  unaffected: the new scope
  carries `fillParentMax*` and `animateItem`, so only the `override` signatures change.
  `RefreshError` is additive and has a default body, so it breaks nothing. The containers also gained
  `isRefreshing`/`onRefresh` after `state`; both default to `null`, so at source level only positional
  callers that passed `content` without the trailing-lambda form need touching — but the composable's
  descriptor changed, so consumers recompile against the new artifact either way (as they must for
  the slot changes above). Tapping retry on a `RefreshError`
  banner dismisses it and starts the reload silently — announcing it would mean a loader over loaded
  content, which is the thing being fixed. The DSL
  builder interfaces are now `Fdk`-prefixed (`FdkPagingScopeBuilder`, `FdkPagingGridScopeBuilder`,
  `FdkPagingSlotsBuilder`); `PagingDefaults` itself keeps its name, like the rest of the defaults
  family. An app that implements only `LoadingDefaults` needs no change at all — the paging fallback
  now delegates its error slots there, which also means raw `e.message` reaches the default paged
  error surface exactly as it already did on the non-paged one.
- **Paged grids** — same states, laid out as tiles, through the *same* `LocalPagingDefaults`: a
  grid is not a second place to word a failure. Two shapes:
  - `Flow<PagingData<T>>.PagingGridContent(columns = GridCells.Adaptive(104.dp), itemKey = { ... })`
    with the same slot DSL — the container twin of `PagingContent` over a `LazyVerticalGrid`,
    pull-to-refresh included.
    `verticalArrangement`/`horizontalArrangement` are per call; `contentPadding` defaults to the
    app-wide `ContentPaddingDefaults` and is overridable per call.
  - `LazyGridScope.pagingItems(items, itemKey = { ... }) { Item { i, x -> ... } }` — the same
    states emitted into a grid the screen **already owns**, for a paged section sharing one
    `LazyVerticalGrid` with a header or a summary card. Takes collected `LazyPagingItems` (the
    caller calls `collectAsLazyPagingItems()`), and the caller's own `isRefreshing` if it drives
    pull-to-refresh itself. The container is a thin wrapper over this.

  Load-state slots are emitted full-span; `Item` keeps `LazyGridItemScope`, so
  `Modifier.animateItem()` still works and a removed tile lets the rest close up. Slot DSL is shared
  with the list (`FdkPagingSlotsBuilder`) — only `Item`/`Prepend` differ, since only they speak the
  layout.

  `LazyListScope.pagingItems(...)` is the list twin of the same extension, for a paged section
  inside a `LazyColumn` the screen owns; `PagingContent` is a thin wrapper over it.
- **`FdkPagingSlotScope`** — the receiver of every paging load-state slot, in `PagingDefaults` and
  in the per-call DSL alike. `LazyItemScope` and `LazyGridItemScope` are unrelated types and only
  the first has `fillParentMax*`, so the slots hang off this intersection instead: `fillParentMaxSize/
  Width/Height` and `animateItem`. In a list every member is the `LazyItemScope` original. In a grid
  `fillParentMaxHeight` needs the viewport, which a lazy grid never hands its items —
  `PagingGridContent` measures it; `pagingItems` takes it as `slotViewport` (build one with
  `BoxWithConstraintsScope.pagingSlotViewport(contentPadding)`) and, unset, lets the slot wrap its
  content, which is what a *section* wants anyway.
- **Snackbars are UI-only**: ViewModels never hold a `SnackbarManager`; they emit events whose type
  implements `SnackbarEvent` (declares its snackbar via the `SnackbarBuilder` DSL).
  `snackbar.ConsumeEvents(viewModel)` consumes only `SnackbarEvent`s; everything else stays pending
  for `EventEffects` — the two compose safely on one screen. Duration follows the action when
  `duration(...)` is not called: `Short` without an `actionLabel`, `Long` with one — a snackbar is
  the only place its action exists, so an undo is not given four seconds to be noticed. Set
  `duration()` explicitly to override; `withDismissAction()` does not affect the choice.
- **Top bars**: `FdKitTopBarTextTitle` / `FdKitTopBarHeadlineTitle` (back arrow via
  `onNavigateBack: (() -> Unit)?` — the callback param is `onNavigateBack`, not `onBack`; pass
  `navigationIcon` to replace the icon entirely), `FdKitFeatureTopBar` (no back default —
  `navigationIcon` defaults to `TopBarDefaults.FeatureNavigationIcon` for an avatar/menu). All read
  `LocalTopBarDefaults` and wire `scrollBehavior` from `ScaffoldSettings`.
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

Extension functions named after their pattern, each taking an explicit `Locale`. The two receivers
have **separate, non-interchangeable** sets:

- `LocalDate`: `ddMMyy`, `ddMM`, `ddMMyyyy`, `ddMMM`, `ddMMMyyyy`, `ddMMMMyyyy`, `MMMdyyyy`,
  `MMMMyyyy`, `EEEE`, `ddMMMOptionalYear`.
- `LocalDateTime`: `Hmm`, `ddMMyyyy`, `ddMMyyyyHmm`, `ddMMOptionalYear`, `ddMMMOptionalYear`,
  `ddMMMMOptionalYear`, `ddMMMHmmOptionalYear`.

Only the listed `*OptionalYear` functions exist — there is no `OptionalYear` variant of every
pattern. They omit the year when it equals the current one (system clock at call time). Plus the
top-level `rangeOptionalYear(locale, start, end)` for `LocalDate` ranges. In Compose, prefer the
`ui-kit` `localizedFormat` wrappers, which resolve the ambient locale.

## 9. Rules checklist

When writing consumer code, enforce:

1. Never swallow `CancellationException` — use `runCatchingRethrowCancellation`/`onError`, and
   plain `try/finally` for must-run cleanup.
2. All ViewModel coroutines go through `task`/`uniqueTask`; keyed variants for supersede-previous
   semantics.
3. State mutations only via `state { }` builder blocks; batch related mutations into one block.
   Blocks are non-suspend by design — load first, then write the result into state.
4. Follow the load/refresh/action recipes: persistent load errors via
   `handledError { state { failed(it) } }`; transient refresh/action errors via `visualError`;
   refresh paths skip `loading()` so content stays visible.
5. Repositories throw typed `HttpError` via `http` (pairs with the ViewModel `Result` recipes);
   `httpSafeCall`/`Either` only where the caller branches on error subtypes. Wrap blocking calls
   in `ioContext`. Map DTOs to display-ready domain types before they reach state.
6. UI observes: `Fetchable` for `RemoteData`, `ErrorEffects` for `ErrorReaction`, `EventEffects` /
   `ConsumeEvents` for one-shot actions, `FdKitRefresh*` containers for `RefreshOwner` — each
   consumer marks its events consumed.
7. Expose read-only contracts to the UI (`StateOwner`, `ErrorEmitter`, `ActionEmitter`); keep the
   mutable managers internal to the ViewModel.
8. Theme app-wide via `FdkScreenDefaults` once; per-call slot parameters override locally.
9. Public FdKit types carry an `Fdk`/`FdKit` prefix (both spellings occur); the `Defaults` theming
   contracts are the exception and are unprefixed.
