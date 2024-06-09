plugins {
    id ("com.android.application")
    id ("org.jetbrains.kotlin.android")
}

// 包名
val packageName = "org.easybangumi.extension"

// 库版本，目前 5.0.3 支持的库版本为 3 到 5
val extensionLibVersion = 7

android {
    namespace = packageName
    compileSdk = 34

    defaultConfig {
        applicationId = packageName
        minSdk =  21
        targetSdk =  34
        versionCode = 7
        versionName = "1.6"

        manifestPlaceholders.put("extensionLibVersion", extensionLibVersion)

    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    dependenciesInfo{
        includeInApk = false
        includeInBundle = false
    }
}


dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    compileOnly("io.github.easybangumiorg:extension-api:1.${extensionLibVersion}-SNAPSHOT")
}
