plugins {
    alias(gradleLibs.plugins.android.test)
    alias(gradleLibs.plugins.androidx.baselineprofile)
}

android {
    namespace = "${AppConfiguration.appId}.baselineprofile"
    compileSdk = AppConfiguration.compileSdk

    defaultConfig {
        minSdk = 28
        targetSdk = AppConfiguration.targetSdk
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
        buildConfigField("String", "TARGET_PACKAGE", "\"${AppConfiguration.applicationId}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    targetProjectPath = ":app"
}

baselineProfile {
    // Android 13+ devices do not need root for profile generation.
    useConnectedDevices = true
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(AppConfiguration.jdk))
    }
}

dependencies {
    implementation(androidx.benchmark.macro.junit4)
    implementation(androidx.test.ext.junit)
    implementation(androidx.test.runner)
    implementation(androidx.test.uiautomator)
}
