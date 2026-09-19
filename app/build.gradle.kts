plugins {
 id("com.android.application")
 kotlin("plugin.serialization")
 id("org.jetbrains.kotlin.plugin.compose")
 id("com.google.devtools.ksp")
}
val stableUpdatePassword = providers.gradleProperty("gruenderelfKeystorePassword").orNull
 ?: System.getenv("GRUENDERELF_KEYSTORE_PASSWORD")
 ?: error("Signing password missing: set GRUENDERELF_KEYSTORE_PASSWORD or -PgruenderelfKeystorePassword")

android {
 namespace="de.gruenderelf.app"
 compileSdk = 36
 buildToolsVersion="36.0.0"
 defaultConfig {
  applicationId="de.gruenderelf.app"; minSdk=26; targetSdk=36
  versionCode=30; versionName="0.4.69"
  testInstrumentationRunner="androidx.test.runner.AndroidJUnitRunner"
 }
 signingConfigs {
  create("stableUpdate") {
   storeFile=file("gruenderelf-update.jks")
   storePassword=stableUpdatePassword
   keyAlias="gruenderelf"
   keyPassword=stableUpdatePassword
  }
 }
 buildTypes {
  debug { isDebuggable=true; signingConfig=signingConfigs.getByName("stableUpdate") }
  release {
   isMinifyEnabled=true; isShrinkResources=true
   signingConfig=signingConfigs.getByName("stableUpdate")
   proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro")
  }
 }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17;targetCompatibility=JavaVersion.VERSION_17 }
 buildFeatures { compose=true }
 packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}";jniLibs.useLegacyPackaging=false }
 testOptions { unitTests.isIncludeAndroidResources=true }
}
kotlin { jvmToolchain(17) }
ksp { arg("room.schemaLocation","$projectDir/schemas") }
dependencies {
 implementation(project(":engine"))
 implementation(platform("androidx.compose:compose-bom:2025.10.01"))
 implementation("androidx.activity:activity-compose:1.11.0")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-core")
 implementation("androidx.navigation:navigation-compose:2.9.5")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
 implementation("androidx.room:room-runtime:2.8.3")
 implementation("androidx.room:room-ktx:2.8.3")
 ksp("androidx.room:room-compiler:2.8.3")
 implementation("androidx.datastore:datastore-preferences:1.1.7")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
 debugImplementation("androidx.compose.ui:ui-tooling")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.robolectric:robolectric:4.16")
 testImplementation("androidx.test:core:1.7.0")
 testImplementation(platform("androidx.compose:compose-bom:2025.10.01"))
 testImplementation("androidx.compose.ui:ui-test-junit4")
 testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
 androidTestImplementation(platform("androidx.compose:compose-bom:2025.10.01"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 androidTestImplementation("androidx.test:runner:1.7.0")
}
