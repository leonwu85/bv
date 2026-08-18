# TV Baseline Profile generator

This module generates a Baseline Profile for the TV cold-start, first top-tab interaction, and
left drawer switching:

1. Cold launch and wait for the first frame.
2. Press DPAD RIGHT (Recommend to Popular).
3. Press DPAD LEFT.
4. Press DPAD RIGHT again.
5. Return to Recommend and press DPAD LEFT to enter the Home drawer item.
6. Press DPAD DOWN to switch Home to UGC, then DPAD RIGHT to enter UGC content.
7. Press DPAD LEFT to return to the drawer, then DPAD UP and DPAD RIGHT to return Home.

The generated journey covers the Popular `LazyGrid`, TopNav focus dispatch, and
`DrawerContent` delayed commit, `KeepAlivePages` page preparation, and content focus restoration
paths. It is a Baseline Profile CUJ, not a Startup Profile CUJ, because most of the interaction
happens after the first displayed frame.

## Generate on a connected Android TV device

Use an Android 13/API 33 or newer TV device or emulator. Only one compatible device should be
connected while generating.

```shell
./gradlew :app:generateBaselineProfile \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile
```

The committed output is written to:

```text
app/src/main/generated/baselineProfiles/baseline-prof.txt
```

The existing `app/src/main/baseline-prof.txt` remains as a small manual fallback. AGP merges it
with the generated profile when packaging R8/release builds.

## Version compatibility

The Baseline Profile plugin and Macrobenchmark are intentionally pinned to `1.5.0-alpha07`.
This repository uses AGP 9.2.1, which is not recognized as a supported Android application
module by the 1.4.1 plugin. Replace the alpha with the first compatible stable 1.5.x release
when it is available.

## CI drift check

Provision and boot an API 33+ Android TV emulator, run the generation command, then fail when the
generated profile is stale:

```shell
git diff --exit-code -- app/src/main/generated/baselineProfiles/baseline-prof.txt
```

The app currently packages only `armeabi-v7a` and `arm64-v8a`, so the CI runner must expose a
compatible ARM Android TV device/emulator. Do not add an x86 ABI to production artifacts merely
to make profile generation run on a generic x86 CI emulator.

Generation is intentionally not attached to normal release assembly, because it requires a device
and builds an additional non-minified profileable target.
