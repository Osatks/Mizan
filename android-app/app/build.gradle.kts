plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// مفتاح التوقيع يُمرَّر من أسرار GitHub (انظر README)، ولا يُحفظ في المستودع
val keystorePath: String? = System.getenv("MIZAN_KEYSTORE")
val hasReleaseKey = keystorePath != null && file(keystorePath).exists()

// رقم الإصدار يزيد مع كل commit تلقائياً، فكل نسخة جديدة تحمل رقماً أعلى (محلياً وفي GitHub)
val buildNumber: Int = runCatching {
    ProcessBuilder("git", "rev-list", "--count", "HEAD").directory(rootDir).redirectErrorStream(true).start()
        .inputStream.bufferedReader().readText().trim().toInt()
}.getOrDefault(0)

android {
    namespace = "com.osatks.mizan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.osatks.mizan"
        minSdk = 26
        targetSdk = 35
        // أعلى من أرقام النسخ السابقة (رقم تشغيل GitHub + 10) حتى يُثبَّت التحديث فوقها
        versionCode = 200 + buildNumber
        versionName = "2.1.$buildNumber"
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("MIZAN_STORE_PASSWORD")
                keyAlias = System.getenv("MIZAN_KEY_ALIAS")
                keyPassword = System.getenv("MIZAN_KEY_PASSWORD")
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
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    // تلاوة القرآن: تشغيل آية آية مع ذاكرة مؤقتة للاستماع دون اتصال
    implementation("androidx.media3:media3-exoplayer:1.4.1")

    testImplementation("junit:junit:4.13.2")
}
