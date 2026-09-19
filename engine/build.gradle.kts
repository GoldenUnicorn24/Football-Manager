plugins { kotlin("jvm"); kotlin("plugin.serialization") }
kotlin { jvmToolchain(17) }
dependencies {
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
 testImplementation(kotlin("test-junit"))
}
tasks.test {
 useJUnit(); exclude("**/SaveProcessProbe.class"); maxHeapSize="1g"
 systemProperty("gruenderelf.test.classpath",sourceSets["test"].runtimeClasspath.asPath)
 testLogging { events("passed","failed","skipped"); showStandardStreams=true }
}
