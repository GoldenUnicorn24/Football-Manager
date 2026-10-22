import java.security.SecureRandom
import java.util.HexFormat
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

plugins {
 id("com.android.application")
 kotlin("plugin.serialization")
 id("org.jetbrains.kotlin.plugin.compose")
 id("com.google.devtools.ksp")
}
val stableUpdatePassword = providers.gradleProperty("gruenderelfKeystorePassword").orNull
 ?: System.getenv("GRUENDERELF_KEYSTORE_PASSWORD")
val stableUpdateKeystore=file("gruenderelf-update.jks")
val hasStableUpdateSigning=stableUpdatePassword!=null&&stableUpdateKeystore.exists()

// Developer-Code (BMW-FC-Modus): kommt aus einem GitHub-Secret bzw. ~/.gradle/gradle.properties.
// In die App gelangt nur ein gesalzener PBKDF2-Hash, nie der Klartext.
val developerCode=(providers.gradleProperty("gruenderelfDevCode").orNull?:System.getenv("GRUENDERELF_DEV_CODE"))?.trim()?.takeIf{it.isNotEmpty()}
val developerSalt=HexFormat.of().formatHex(ByteArray(16).also{SecureRandom().nextBytes(it)})
val developerHash=developerCode?.let{code->
 val spec=PBEKeySpec(code.toCharArray(),HexFormat.of().parseHex(developerSalt),120_000,256)
 HexFormat.of().formatHex(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded)
}?:""

android {
 namespace="de.gruenderelf.app"
 compileSdk = 36
 buildToolsVersion="36.0.0"
 defaultConfig {
  applicationId="de.gruenderelf.app"; minSdk=26; targetSdk=36
  versionCode=74; versionName="0.5.25"
  testInstrumentationRunner="androidx.test.runner.AndroidJUnitRunner"
  buildConfigField("String","DEV_CODE_HASH","\"$developerHash\"")
  buildConfigField("String","DEV_CODE_SALT","\"$developerSalt\"")
 }
 signingConfigs {
  if(hasStableUpdateSigning)create("stableUpdate") {
   storeFile=stableUpdateKeystore
   storePassword=stableUpdatePassword
   keyAlias="gruenderelf"
   keyPassword=stableUpdatePassword
  }
 }
 buildTypes {
  debug {
   isDebuggable=true
   if(hasStableUpdateSigning)signingConfig=signingConfigs.getByName("stableUpdate")
  }
  release {
   isMinifyEnabled=true; isShrinkResources=true
   if(hasStableUpdateSigning)signingConfig=signingConfigs.getByName("stableUpdate")
   proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro")
  }
 }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17;targetCompatibility=JavaVersion.VERSION_17 }
 buildFeatures { compose=true; buildConfig=true }
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
