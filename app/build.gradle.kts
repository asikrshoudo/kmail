import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

// Single source of truth: version.properties (MAJOR.MINOR.PATCH, each part 0..99).
val versionParts: List<Int> = Properties()
    .apply { rootProject.file("version.properties").inputStream().use { load(it) } }
    .getProperty("VERSION_NAME")
    ?.split(".")
    ?.map { it.toInt() }
    ?: error("VERSION_NAME missing in version.properties")
require(versionParts.size == 3 && versionParts.all { it in 0..99 }) {
    "VERSION_NAME must be MAJOR.MINOR.PATCH with each part between 0 and 99"
}

android {
    namespace = "com.kmail.thekami"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kmail.thekami"
        minSdk = 26
        targetSdk = 37
        versionCode = versionParts[0] * 10_000 + versionParts[1] * 100 + versionParts[2]
        versionName = versionParts.joinToString(".")
    }

    androidResources {
        localeFilters += listOf("en", "de", "ja", "bn", "hi", "ar", "ko")
    }

    // Signing values exist only in the release workflow (GitHub Secrets as env variables).
    signingConfigs {
        System.getenv("KMAIL_KEYSTORE_PATH")?.let { path ->
            create("release") {
                storeFile = file(path)
                storePassword = System.getenv("KMAIL_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KMAIL_KEY_ALIAS")
                keyPassword = System.getenv("KMAIL_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
        }
    }

    // Angus Mail ships duplicate license files.
    packaging {
        resources {
            pickFirsts += setOf("META-INF/LICENSE.md", "META-INF/NOTICE.md")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    implementation(libs.room.runtime)
    implementation(libs.jakarta.mail.api)
    implementation(libs.angus.mail)
    implementation(libs.angus.activation)
    ksp(libs.hilt.compiler)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

room {
    schemaDirectory("$projectDir/schemas")
}
