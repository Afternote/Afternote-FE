import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const repositoryRoot = new URL("../../", import.meta.url);
const readRepositoryFile = (path) => readFile(new URL(path, repositoryRoot), "utf8");

test("Baseline Profile uses the AGP 9 compatible producer and explicit consumer wiring", async () => {
    const [catalog, settings, app, producer] = await Promise.all([
        readRepositoryFile("gradle/libs.versions.toml"),
        readRepositoryFile("settings.gradle.kts"),
        readRepositoryFile("app/build.gradle.kts"),
        readRepositoryFile("baselineprofile/build.gradle.kts"),
    ]);

    assert.match(catalog, /^baselineProfile = "1\.5\.0-rc02"$/m);
    assert.match(catalog, /^profileInstaller = "1\.4\.1"$/m);
    assert.match(catalog, /androidx-benchmark-macro-junit4.*version\.ref = "baselineProfile"/);
    assert.match(catalog, /androidx-profileinstaller.*version\.ref = "profileInstaller"/);
    assert.match(catalog, /androidx-baselineprofile = \{ id = "androidx\.baselineprofile", version\.ref = "baselineProfile" \}/);
    assert.match(settings, /include\(":baselineprofile"\)/);

    assert.match(app, /alias\(libs\.plugins\.androidx\.baselineprofile\)/);
    assert.match(app, /implementation\(libs\.androidx\.profileinstaller\)/);
    assert.match(app, /baselineProfile\(project\(":baselineprofile"\)\)/);
    assert.match(app, /automaticGenerationDuringBuild = false/);
    assert.match(app, /mergeIntoMain = true/);
    assert.match(app, /saveInSrc = true/);
    assert.match(app, /dexLayoutOptimization = true/);

    assert.match(producer, /id\("com\.android\.test"\)/);
    assert.match(producer, /alias\(libs\.plugins\.androidx\.baselineprofile\)/);
    assert.match(producer, /targetProjectPath = ":app"/);
    assert.match(producer, /android\.experimental\.self-instrumenting/);
    assert.match(
        producer,
        /create\("pixel2Api34"\)[\s\S]*?apiLevel = 34[\s\S]*?systemImageSource = "aosp"/,
    );
    assert.match(producer, /managedDevices \+= "pixel2Api34"/);
    assert.match(producer, /useConnectedDevices = false/);
    assert.match(producer, /implementation\(libs\.androidx\.benchmark\.macro\.junit4\)/);
});

test("generator records startup without an absolute performance threshold", async () => {
    const [generator, profile] = await Promise.all([
        readRepositoryFile(
            "baselineprofile/src/main/java/com/afternote/baselineprofile/BaselineProfileGenerator.kt",
        ),
        readRepositoryFile("app/src/main/generated/baselineProfiles/baseline-prof.txt"),
    ]);

    assert.match(generator, /BaselineProfileRule\(\)/);
    assert.match(generator, /packageName = "com\.afternote\.afternote_fe"/);
    assert.match(generator, /includeInStartupProfile = true/);
    assert.match(generator, /pressHome\(\)/);
    assert.match(generator, /startActivityAndWait\(\)/);
    assert.doesNotMatch(generator, /measureRepeated|timeToInitialDisplay|threshold/i);

    assert.match(profile, /^Lcom\/afternote\/afternote_fe\/GlobalApplication;$/m);
    assert.match(profile, /^Lcom\/afternote\/afternote_fe\/MainActivity;$/m);
    assert.doesNotMatch(profile, /todo|placeholder/i);
});

test("API boundary devices are explicit and the smoke remains valid for existing full lanes", async () => {
    const [app, gradleProperties, smoke] = await Promise.all([
        readRepositoryFile("app/build.gradle.kts"),
        readRepositoryFile("gradle.properties"),
        readRepositoryFile(
            "app/src/androidTest/java/com/afternote/afternote_fe/ApiBoundarySmokeAndroidTest.kt",
        ),
    ]);

    for (const api of [26, 36]) {
        assert.match(
            app,
            new RegExp(
                `create\\("pixel2Api${api}"\\)[\\s\\S]*?apiLevel = ${api}[\\s\\S]*?systemImageSource = "aosp"`,
            ),
        );
    }
    assert.match(
        gradleProperties,
        /^android\.experimental\.testOptions\.managedDevices\.allowOldApiLevelDevices=true$/m,
    );
    assert.match(smoke, /Build\.VERSION\.SDK_INT in 26\.\.36/);
    assert.match(smoke, /onboarding_welcome_start/);
    assert.match(smoke, /FailureArtifactRule/);
    assert.match(smoke, /captureToImage\(\)\.asAndroidBitmap\(\)/);
    assert.doesNotMatch(smoke, /SdkSuppress/);
});

test("workflow generates only on the default branch and validates PR packaging without an emulator", async () => {
    const workflow = await readRepositoryFile(".github/workflows/baseline-profile.yml");

    assert.match(workflow, /^  pull_request:\n    paths:$/m);
    assert.match(workflow, /^  schedule:\n    - cron: '[^']+'$/m);
    assert.match(workflow, /^  workflow_dispatch:$/m);
    assert.match(workflow, /^permissions:\n  contents: read$/m);
    assert.doesNotMatch(workflow, /contents: write|pull-requests: write/);
    assert.match(workflow, /github\.event\.pull_request\.head\.repo\.full_name == github\.repository/);
    assert.match(workflow, /github\.ref_name == github\.event\.repository\.default_branch/);
    assert.match(workflow, /persist-credentials: false/);
    assert.match(workflow, /ref: \$\{\{ github\.event_name == 'pull_request' && github\.event\.pull_request\.head\.sha \|\| github\.sha \}\}/);
    assert.match(workflow, /uses: \.\/\.github\/actions\/setup-ci-config/);
    assert.match(workflow, /uses: \.\/\.github\/actions\/setup-ci-release-signing/);

    const generation = workflow.slice(
        workflow.indexOf("- name: Enable and verify KVM access"),
        workflow.indexOf("- name: Build release AAB without implicit profile generation"),
    );
    assert.match(generation, /if: github\.event_name != 'pull_request'/g);
    assert.match(generation, /:app:generateBaselineProfile/);
    assert.match(generation, /git diff --exit-code/);
    assert.match(generation, /git status --porcelain --untracked-files=all/);

    assert.match(workflow, /:app:bundleRelease/);
    assert.match(workflow, /android\.baselineProfile\.automaticGenerationDuringBuild=false/);
    assert.match(workflow, /-x :app:uploadCrashlyticsMappingFileRelease/);
    assert.match(
        workflow,
        /BUNDLE-METADATA\/com\.android\.tools\.build\.profiles\/baseline\.prof/,
    );
    assert.match(workflow, /profile_bytes/);
    assert.doesNotMatch(workflow, /timeToInitialDisplay|startupMs|performance[_ -]?threshold/i);
});
