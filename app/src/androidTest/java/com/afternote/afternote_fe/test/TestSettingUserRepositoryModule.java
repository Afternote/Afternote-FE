package com.afternote.afternote_fe.test;

import com.afternote.feature.setting.data.di.SettingUserRepositoryModule;
import com.afternote.feature.setting.domain.SettingAccountRepository;
import com.afternote.feature.setting.domain.SettingNotificationRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.testing.TestInstallIn;

/**
 * 설정 계정·알림 저장소를 계측 테스트의 fake 바인딩으로 교체한다.
 *
 * <p>Kotlin internal 클래스는 JVM에서 public이므로 Java 테스트에서 참조할 수 있다.
 * 프로덕션 Hilt 모듈의 Kotlin 공개 범위를 유지하기 위해 이 대체 모듈만 Java로 작성한다.
 */
@Module
@TestInstallIn(components = SingletonComponent.class, replaces = SettingUserRepositoryModule.class)
public final class TestSettingUserRepositoryModule {
    private TestSettingUserRepositoryModule() {}

    @Provides
    @Singleton
    static SettingAccountRepository provideSettingAccountRepository() {
        return TestFakesKt.appTestSettingAccountRepository();
    }

    @Provides
    @Singleton
    static SettingNotificationRepository provideSettingNotificationRepository() {
        return TestFakesKt.appTestSettingNotificationRepository();
    }
}
