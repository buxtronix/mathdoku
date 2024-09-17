plugins {
    id("com.android.application")
}

android {
    namespace = "net.cactii.mathdoku"

    val minAndroidVersion = 25
    val androidVersion = 35
    val javaVersion = JavaVersion.VERSION_21

    compileSdk = androidVersion
    // https://developer.android.com/studio/releases/build-tools
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = namespace
        minSdk = minAndroidVersion
        targetSdk = androidVersion
        versionCode = 600
        versionName = "2.13"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        compileOptions.encoding = Charsets.UTF_8.name()

        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }

    applicationVariants.all {
        outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output?.outputFileName = "${namespace}-${name}.apk"
        }
    }

    lint {
        abortOnError = false
    }

}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}
