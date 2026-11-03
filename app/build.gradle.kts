plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

android {
  val optimizedSmoke = providers.gradleProperty("optimizedSmoke").orNull == "true"
  val privateUpdate = providers.gradleProperty("privateUpdate").orNull == "true"
  if (optimizedSmoke) {
    testBuildType = "release"
    sourceSets.getByName("androidTest").java.setSrcDirs(listOf("src/optimizedSmoke/java"))
    sourceSets.getByName("androidTest").kotlin.setSrcDirs(listOf("src/optimizedSmoke/java"))
    sourceSets.getByName("androidTest").manifest.srcFile("src/optimizedSmoke/AndroidManifest.xml")
  }
  namespace = "com.steadfast.app"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.steadfast.app"
    minSdk = 29
    targetSdk = 36
    versionCode = 2
    versionName = "1.1.0"

    testInstrumentationRunner = if (optimizedSmoke) "com.steadfast.app.smoke.OptimizedSmokeRunner" else "androidx.test.runner.AndroidJUnitRunner"
  }

  splits {
    abi {
      isEnable = true
      reset()
      include("arm64-v8a")
      isUniversalApk = true
    }
  }

  signingConfigs {
    if (!System.getenv("KEYSTORE_PATH").isNullOrBlank()) create("release") {
      storeFile = file(System.getenv("KEYSTORE_PATH"))
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = if (optimizedSmoke || privateUpdate) signingConfigs.getByName("debug") else signingConfigs.findByName("release")
      if (optimizedSmoke) testProguardFiles("optimized-smoke-test-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      all {
        // Isolate Robolectric native-rendering state between classes and keep memory bounded.
        it.forkEvery = 1
        it.maxParallelForks = 1
      }
    }
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.moshi.kotlin)

  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)

  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)

  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)

  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
