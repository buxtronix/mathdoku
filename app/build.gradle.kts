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
        versionName = "2.12"
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

    lint {
        abortOnError = false
    }

}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("com.android.support.test:runner:1.0.2")
    androidTestImplementation("com.android.support.test.espresso:espresso-core:3.0.2")
}
