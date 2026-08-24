@file:Suppress("UnstableApiUsage")

import com.android.build.api.variant.FilterConfiguration.FilterType
import com.android.build.api.variant.impl.VariantOutputImpl
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(gradleLibs.plugins.android.application)
    alias(gradleLibs.plugins.androidx.baselineprofile)
    alias(gradleLibs.plugins.compose.compiler)
    alias(gradleLibs.plugins.firebase.crashlytics)
    alias(gradleLibs.plugins.google.ksp)
    alias(gradleLibs.plugins.google.services)
    alias(gradleLibs.plugins.kotlin.serialization)
}

val appVersion = AppConfiguration.resolveVersion(rootProject.projectDir)
val googleServicesAvailable = AppConfiguration.isGoogleServicesAvailable(rootProject.projectDir)

val signingProp = file(project.rootProject.file("signing.properties"))

android {
    signingConfigs {
        if (signingProp.exists()) {
            val properties = Properties().apply {
                load(FileInputStream(signingProp))
            }
            create("key") {
                storeFile = rootProject.file(properties.getProperty("keystore.path"))
                storePassword = properties.getProperty("keystore.pwd")
                keyAlias = properties.getProperty("keystore.alias")
                keyPassword = properties.getProperty("keystore.alias_pwd")
            }
        }
    }

    namespace = AppConfiguration.appId
    compileSdk = AppConfiguration.compileSdk

    defaultConfig {
        applicationId = AppConfiguration.applicationId
        minSdk = AppConfiguration.minSdk
        targetSdk = AppConfiguration.targetSdk
        versionCode = appVersion.code
        versionName = appVersion.name

        // 只打包 ARM 架构，减少 APK 体积（排除 x86 和 x86_64）
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    flavorDimensions.add("channel")

    productFlavors {
        // create("lite") {
        //     dimension = "channel"
        // }
        create("default") {
            dimension = "channel"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true // 移除未使用的资源
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingProp.exists()) signingConfig = signingConfigs.getByName("key")
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = googleServicesAvailable
            }
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".debug"
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
        create("r8Test") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".r8test"
            signingConfig = signingConfigs.getByName(if (signingProp.exists()) "key" else "debug")
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
        create("alpha") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingProp.exists()) signingConfig = signingConfigs.getByName("key")
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = googleServicesAvailable
            }
        }
    }

    buildFeatures {
        compose = true
        //buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "**/*.proto"
            excludes += "**/*.kotlin_metadata"
            excludes += "**/*.txt"
            excludes += "**/*.version"
        }

        jniLibs {
            useLegacyPackaging = false
            // 排除 VLC 的 .so 文件，使用按需下载的库
            val vlcLibs = listOf("libvlc", "libc++_shared", "libvlcjni")
            val abis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            vlcLibs.forEach { vlcLibName -> abis.forEach { abi -> excludes.add("lib/$abi/$vlcLibName.so") } }
        }

        if (gradle.startParameter.taskNames.find { it.startsWith("assembleLite") } != null) {
            jniLibs {
                val vlcLibs = listOf("libvlc", "libc++_shared", "libvlcjni")
                val abis = listOf("x86_64", "x86", "arm64-v8a", "armeabi-v7a")
                vlcLibs.forEach { vlcLibName -> abis.forEach { abi -> excludes.add("lib/$abi/$vlcLibName.so") } }
            }
        }
    }

    // 使用 ABI Filters 替代 Splits，在 defaultConfig 中配置

}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            val abi = output.filters.find { it.filterType == FilterType.ABI }?.identifier ?: "universal"
            (output as VariantOutputImpl).outputFileName.set(
                "BV_${appVersion.code}_${appVersion.name}.${variant.buildType}_${variant.flavorName}_$abi.apk"
            )
            output.versionName.set("${appVersion.name}.${variant.buildType}")
        }
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_build_reports")
    stabilityConfigurationFiles.addAll(
        layout.projectDirectory.file("compose_compiler_config.conf")
    )
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(AppConfiguration.jdk))
    }
}

dependencies {
    implementation(project(":app:mobile"))
    implementation(project(":app:tv"))
    implementation(project(":app:shared"))
    implementation(androidx.profileinstaller)
    implementation(libs.vlc.android.all)
}

baselineProfile {
    // Only release needs to run the producer. The generated file lives in src/main,
    // so R8Test and Alpha consume the same committed rules without duplicate captures.
    variants {
        create("release") {
            from(project(":baselineprofile"))
        }
    }

    // Keep a single generated profile shared by all release variants and commit it.
    mergeIntoMain = true
    saveInSrc = true

    // CI regenerates explicitly. Normal release/R8 builds must not require a device.
    automaticGenerationDuringBuild = false

    warnings {
        // Alpha/R8Test consume the committed src/main output but do not run the producer.
        variantHasNoBaselineProfileDependency = false
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
