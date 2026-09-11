package com.afternote.feature.setting.data

import com.afternote.feature.setting.domain.Passkey
import com.afternote.feature.setting.domain.PasskeyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Singleton

/** app 계측은 이 fixture를 통해 internal 프로덕션 모듈을 대체한다. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [PasskeyModule::class])
public object PasskeyTestModule {
    @Provides
    @Singleton
    public fun provideScenario(): PasskeyTestScenario = PasskeyTestScenario()

    @Provides
    public fun providePasskeyRepository(scenario: PasskeyTestScenario): PasskeyRepository = scenario
}

public class PasskeyTestScenario : PasskeyRepository {
    @Volatile
    public var passkeys: List<Passkey> = emptyList()

    @Volatile
    public var listFails: Boolean = false

    public val listCalls: AtomicInteger = AtomicInteger()
    public val optionsCalls: AtomicInteger = AtomicInteger()
    public val registeredCredentials: CopyOnWriteArrayList<String> = CopyOnWriteArrayList<String>()

    override suspend fun getPasskeys(): List<Passkey> {
        listCalls.incrementAndGet()
        if (listFails) throw IOException("Controlled passkey list failure")
        return passkeys
    }

    override suspend fun getRegistrationOptions(): String {
        optionsCalls.incrementAndGet()
        return """
            {
              "challenge": "YWZ0ZXJub3RlLXRlc3QtY2hhbGxlbmdl",
              "rp": {"name": "Afternote test", "id": "example.test"},
              "user": {"id": "dGVzdA", "name": "test@example.test", "displayName": "Test"},
              "pubKeyCredParams": [{"type": "public-key", "alg": -7}],
              "timeout": 1000,
              "attestation": "none"
            }
            """.trimIndent()
    }

    override suspend fun registerPasskey(credentialJson: String): Passkey {
        registeredCredentials += credentialJson
        error("Real credentials must not be registered by this fixture")
    }
}
