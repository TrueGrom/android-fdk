package grmv.android.fdk.state.traits

import androidx.compose.runtime.Immutable
import grmv.android.fdk.state.BaseState
import grmv.android.fdk.state.BuilderOps
import grmv.android.fdk.state.RemoteData

/**
 * State marker: holds a single [RemoteData] payload of type [T] that tracks the lifecycle of a
 * remote request (loading / fetched / failed).
 *
 * Implement on a state data class to opt in to [RemoteDataOps]:
 * ```
 * data class ProfileState(
 *     override val remoteData: RemoteData<Profile> = RemoteData.loading(),
 * ) : RemoteDataState<ProfileState, Profile> {
 *     override fun withRemoteData(remoteData: RemoteData<Profile>) = copy(remoteData = remoteData)
 * }
 * ```
 *
 * A state can hold at most one `RemoteDataState` payload (Kotlin disallows multiple instantiations
 * of the same generic supertype). For multi-holder states, hand-roll trait-equivalent operations
 * keyed per holder.
 *
 * @param S concrete state type (F-bounded self type so [withRemoteData] returns [S]).
 * @param T payload type carried by [RemoteData.Fetched].
 */
@Immutable
interface RemoteDataState<S : RemoteDataState<S, T>, T> : BaseState {
    val remoteData: RemoteData<T>
    fun withRemoteData(remoteData: RemoteData<T>): S
}

/**
 * Reusable mutation surface for any [RemoteDataState].
 *
 * Mix into a builder to drive the [RemoteData] lifecycle:
 * ```
 * class ProfileStateBuilder(override val initial: ProfileState) :
 *     BaseStateBuilder<ProfileState>(),
 *     RemoteDataOps<ProfileState, Profile>
 * ```
 *
 * Method names mirror [RemoteData]'s sealed cases (`Loading`, `Fetched`, `Error`), exposed as
 * imperative builder verbs.
 */
interface RemoteDataOps<S : RemoteDataState<S, T>, T> : BuilderOps<S> {
    /** Replaces the payload with [RemoteData.Loading]. */
    fun loading() = accumulate { it.withRemoteData(RemoteData.loading()) }

    /** Replaces the payload with [RemoteData.Fetched] carrying [data]. */
    fun fetched(data: T) = accumulate { it.withRemoteData(RemoteData.fetched(data)) }

    /** Replaces the payload with [RemoteData.Error] carrying [error]. */
    fun failed(error: Throwable) = accumulate { it.withRemoteData(RemoteData.error(error)) }
}
