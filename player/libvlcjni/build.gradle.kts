plugins {
    alias(gradleLibs.plugins.android.library)
}

/**
 * libvlc-android Java 层（libvlcjni），源码入库并做了 VLC 3 / VLC 4 双版本兼容。
 *
 * 原生库（libvlc.so / libvlcjni.so / libc++_shared.so）不随 APK 打包，由应用在运行时按用户选择的
 * 版本从 Maven 下载 `libvlc-all` AAR 并解出对应 ABI 的 .so（见 app/shared 的 VlcLibsInstaller）。
 * 详见同目录 README.md。
 */
android {
    namespace = "org.videolan"
    compileSdk = AppConfiguration.compileSdk

    defaultConfig {
        minSdk = AppConfiguration.minSdk
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        create("r8Test") {
            isMinifyEnabled = false
        }
        create("alpha") {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        // util/DisplayManager 使用 BuildConfig.DEBUG
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // 上游代码，不按本项目 lint 规则整改
        abortOnError = false
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(AppConfiguration.jdk))
    }
}

dependencies {
    api(androidx.annotation)
    implementation(androidx.core.ktx)
    implementation(androidx.lifecycle.livedata)
}
