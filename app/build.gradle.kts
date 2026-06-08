import java.util.Properties
import java.util.Base64
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun configValue(name: String, defaultValue: String = ""): String {
    return (findProperty(name) as String?)
        ?: localProperties.getProperty(name)
        ?: System.getenv(name)
        ?: defaultValue
}

fun String.asBuildConfigString(): String {
    return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

fun String.asObfuscatedBuildConfigString(): String {
    if (isBlank()) return "\"\""
    val obfuscated = toByteArray(Charsets.UTF_8)
        .mapIndexed { index, byte ->
            (byte.toInt() xor 0x5A xor ((index * 31 + 17) and 0xFF)).toByte()
        }
        .toByteArray()
    return Base64.getEncoder().encodeToString(obfuscated).asBuildConfigString()
}

val appVersionCode = (findProperty("VERSION_CODE") as String?)?.toIntOrNull() ?: 1
val appVersionName = (findProperty("VERSION_NAME") as String?) ?: "1.0.0"
val aiApiBaseUrl = configValue("AI_API_BASE_URL", "https://api.edgefn.net/v1")
val aiModelName = configValue("AI_MODEL_NAME", "DeepSeek-V3.2")
val baiduAsrAppId = configValue("BAIDU_ASR_APP_ID")
val baiduAsrApiKey = configValue("BAIDU_ASR_API_KEY")
val baiduAsrSecretKey = configValue("BAIDU_ASR_SECRET_KEY")
val baiduAsrDevPid = configValue("BAIDU_ASR_DEV_PID", "1537").toIntOrNull() ?: 1537
val releaseKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val releaseKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.jishiyong"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jishiyong"
        minSdk = 26
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName

        buildConfigField("String", "GITHUB_REPOSITORY_NAME", "\"zarttic/jishiyong\"")
        buildConfigField("String", "AI_API_BASE_URL", aiApiBaseUrl.asBuildConfigString())
        buildConfigField("String", "AI_MODEL_NAME", aiModelName.asBuildConfigString())
        buildConfigField("String", "BAIDU_ASR_APP_ID", baiduAsrAppId.asBuildConfigString())
        buildConfigField("String", "BAIDU_ASR_API_KEY_OBFUSCATED", baiduAsrApiKey.asObfuscatedBuildConfigString())
        buildConfigField("String", "BAIDU_ASR_SECRET_KEY_OBFUSCATED", baiduAsrSecretKey.asObfuscatedBuildConfigString())
        buildConfigField("int", "BAIDU_ASR_DEV_PID", baiduAsrDevPid.toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // v2.4.0 was the first R8-minified release APK and crashes on launch.
            // Keep release unminified until minified APKs are covered by device smoke tests.
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Coil
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Networking
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json)
}

tasks.register("verifyBaiduAsrReleaseConfig") {
    group = "verification"
    description = "Fails release builds when Baidu cloud speech credentials are missing."
    doLast {
        val missing = listOf(
            "BAIDU_ASR_APP_ID" to baiduAsrAppId,
            "BAIDU_ASR_API_KEY" to baiduAsrApiKey,
            "BAIDU_ASR_SECRET_KEY" to baiduAsrSecretKey
        ).filter { (_, value) -> value.isBlank() }

        if (missing.isNotEmpty()) {
            throw GradleException(
                "Missing Baidu ASR release config: " + missing.joinToString { it.first } +
                    ". Configure these values in GitHub Secrets, local.properties, environment variables, or Gradle properties before building a release APK."
            )
        }
    }
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn("verifyBaiduAsrReleaseConfig")
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("failed", "skipped")
        exceptionFormat = TestExceptionFormat.FULL
    }
}
