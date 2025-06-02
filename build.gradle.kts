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
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "motioncut"
            packageVersion = "1.0.0"
            
            // Platform-specific configurations
            macOS {
                bundleID = "com.yanbin.motioncut"
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
            }
            
            windows {
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
                upgradeUuid = "18159995-d967-4CD2-8885-77BFA97CFA9F"
            }
            
            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
        }
        
        buildTypes.release.proguard {
            configurationFiles.from("compose-desktop.pro")
        }
    }
}
