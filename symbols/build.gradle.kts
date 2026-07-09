plugins {
    alias(gradleLibs.plugins.kotlin.jvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(AppConfiguration.jdk))
    }
}

dependencies {
    // 图标路径已入库，不再依赖 material-symbols-compose KSP（其编译期需联网拉取 SVG）
    compileOnly(androidx.compose.ui)
}
