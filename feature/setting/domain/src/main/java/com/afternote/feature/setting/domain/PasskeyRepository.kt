package com.afternote.feature.setting.domain

public data class Passkey(
    val id: Long,
    val displayName: String,
    val createdAt: String,
)

/** 설정의 패스키 목록·등록 계약. 서버가 자격 증명 등록 여부의 정본이다. */
public interface PasskeyRepository {
    public suspend fun getPasskeys(): List<Passkey>

    /** Credential Manager에 전달할 PublicKeyCredentialCreationOptions JSON. */
    public suspend fun getRegistrationOptions(): String

    /** Credential Manager가 생성한 PublicKeyCredential JSON을 서버에서 검증·등록한다. */
    public suspend fun registerPasskey(credentialJson: String): Passkey
}
