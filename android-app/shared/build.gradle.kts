plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

/**
 * كل منطق ميزان وواجهاته (Compose Multiplatform): يُستعمل في تطبيق أندرويد (:app) وتطبيق iOS (iosApp).
 * ما يخص كل نظام (التخزين، الشبكة، الصوت، الموقع، البوصلة، الإشعارات) في androidMain و iosMain.
 */
kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { t ->
        t.binaries.framework {
            baseName = "Shared"
            isStatic = false
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.materialIconsExtended)
            api("org.jetbrains.compose.ui:ui-backhandler:1.8.2")
            api("org.jetbrains.androidx.navigation:navigation-compose:2.9.0-beta03")
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            implementation("org.jetbrains.kotlinx:atomicfu:0.27.0")
        }
        androidMain.dependencies {
            api("androidx.core:core-ktx:1.15.0")
            api("androidx.activity:activity-compose:1.10.1")
            // تلاوة القرآن: تشغيل آية آية مع ذاكرة مؤقتة للاستماع دون اتصال
            implementation("androidx.media3:media3-exoplayer:1.4.1")
        }
        val androidUnitTest by getting {
            dependencies { implementation("junit:junit:4.13.2") }
        }
    }
}

android {
    namespace = "com.osatks.mizan.shared"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    // نصوص المصحف والتفاسير والأذكار وحزم اللغات: مجلد واحد يُضمَّن في أندرويد وفي حزمة iOS
    sourceSets["main"].assets.srcDirs("src/commonMain/assets")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}

