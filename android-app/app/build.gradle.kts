plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// مفتاح التوقيع يُمرَّر من أسرار GitHub (انظر README)، ولا يُحفظ في المستودع
val localKeystore = rootProject.file("mizan-upload-key.jks")
val keystorePath: String? = System.getenv("MIZAN_KEYSTORE") ?: if (localKeystore.exists()) localKeystore.absolutePath else null
val storePasswordEnv = System.getenv("MIZAN_STORE_PASSWORD") ?: "mizan2026"
val keyAliasEnv = System.getenv("MIZAN_KEY_ALIAS") ?: "mizan"
val keyPasswordEnv = System.getenv("MIZAN_KEY_PASSWORD") ?: "mizan2026"
val hasReleaseKey = keystorePath != null && file(keystorePath).exists()

// رقم الإصدار يزيد مع كل commit تلقائياً، فكل نسخة جديدة تحمل رقماً أعلى (محلياً وفي GitHub)
val buildNumber: Int = runCatching {
    ProcessBuilder("git", "rev-list", "--count", "HEAD").directory(rootDir).redirectErrorStream(true).start()
        .inputStream.bufferedReader().readText().trim().toInt()
}.getOrDefault(0)

android {
    namespace = "com.osatks.mizan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mizan.almuslim"
        minSdk = 26
        targetSdk = 36
        // أعلى من أرقام النسخ السابقة (رقم تشغيل GitHub + 10) حتى يُثبَّت التحديث فوقها
        versionCode = 201 + buildNumber
        val patch = (buildNumber - 53).coerceAtLeast(1)
        versionName = "2.2.$patch"
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = storePasswordEnv
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            }
        }
    }

    // ملف النسخة باسم رقمها: mizan-2.1.30-release.apk
    applicationVariants.all {
        val v = versionName
        outputs.all { (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = "mizan-$v-${buildType.name}.apk" }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("release")
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
    }
}

dependencies {
    // كل الشاشات والمنطق في :shared (Compose Multiplatform)، والتطبيق يضيف ما يخص أندرويد فقط
    implementation(project(":shared"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.8.2")
}
