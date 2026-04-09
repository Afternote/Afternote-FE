package com.afternote.core.common.dev

/**
 * Single compile-time switch for fake vs real implementations across modules.
 *
 * - `true`: emulator / UI work — fake auth, fake editor data, stub-backed receiver ports (explicit fakes).
 * - `false`: real API paths (e.g. `AuthRepositoryImpl`, real editor providers); receiver ports use `Real*`
 *   types in DI (currently delegating to stubs until APIs are wired).
 */
object DevEnvironment {
    const val USE_FAKE_DATA: Boolean = false
}
