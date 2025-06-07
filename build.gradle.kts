import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "com.yanbin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

kotlin {
    jvmToolchain(17)
    
    jvm("desktop")
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                // JavaCV for optimized video processing with hardware acceleration
                implementation("org.bytedeco:javacv-platform:1.5.9")
                // Coroutines for video playback
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        
        // JVM arguments for optimized video playback
        jvmArgs += listOf(
            "-Djava.awt.headless=false",  // Ensure we're not in headless mode
            "-XX:+UseG1GC",               // Use G1 garbage collector for better performance
            "-XX:MaxGCPauseMillis=10"     // Minimize GC pauses for smooth video playback
        )
        
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "motioncut"
            packageVersion = "1.0.0"
            
            // Platform-specific configurations
            macOS {
                bundleID = "com.yanbin.motioncut"
            }

            windows {
                upgradeUuid = "18159995-d967-4CD2-8885-77BFA97CFA9F"
            }

            linux {
            }
        }
        
        buildTypes.release.proguard {
            configurationFiles.from("compose-desktop.pro")
        }
    }
}
