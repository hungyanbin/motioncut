package com.yanbin.motioncut.platform

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Desktop implementation of OSInfo that detects the platform at runtime
 */
class DesktopOSInfo : OSInfo {
    private val osName = System.getProperty("os.name").lowercase()
    
    override fun getOSName(): String {
        return when {
            osName.contains("mac") || osName.contains("darwin") -> "macOS"
            osName.contains("win") -> "Windows"
            osName.contains("linux") -> "Linux"
            else -> System.getProperty("os.name")
        }
    }
    
    override fun getOSVersion(): String {
        return when {
            osName.contains("mac") || osName.contains("darwin") -> getMacOSVersion()
            osName.contains("win") -> getWindowsVersion()
            osName.contains("linux") -> getLinuxVersion()
            else -> System.getProperty("os.version") ?: "Unknown"
        }
    }
    
    private fun getMacOSVersion(): String {
        return try {
            // Get macOS version using sw_vers command
            val process = ProcessBuilder("sw_vers", "-productVersion").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val version = reader.readLine() ?: "Unknown"
            reader.close()
            process.waitFor()
            
            // Also get the build version for more detailed info
            val buildProcess = ProcessBuilder("sw_vers", "-buildVersion").start()
            val buildReader = BufferedReader(InputStreamReader(buildProcess.inputStream))
            val buildVersion = buildReader.readLine() ?: ""
            buildReader.close()
            buildProcess.waitFor()
            
            if (buildVersion.isNotEmpty()) {
                "$version (Build $buildVersion)"
            } else {
                version
            }
        } catch (e: Exception) {
            // Fallback to system property
            System.getProperty("os.version") ?: "Unknown"
        }
    }
    
    private fun getWindowsVersion(): String {
        return try {
            // Get Windows version using wmic command
            val process = ProcessBuilder("cmd", "/c", "wmic os get Caption,Version /format:list").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            var caption = ""
            var version = ""
            
            reader.useLines { lines ->
                lines.forEach { line ->
                    when {
                        line.startsWith("Caption=") -> {
                            caption = line.substringAfter("Caption=").trim()
                        }
                        line.startsWith("Version=") -> {
                            version = line.substringAfter("Version=").trim()
                        }
                    }
                }
            }
            
            process.waitFor()
            
            // Format the result
            when {
                caption.isNotEmpty() && version.isNotEmpty() -> {
                    // Extract just the Windows version name from caption
                    val windowsName = caption.replace("Microsoft ", "")
                    "$windowsName (Version $version)"
                }
                version.isNotEmpty() -> version
                caption.isNotEmpty() -> caption.replace("Microsoft ", "")
                else -> "Unknown"
            }
        } catch (e: Exception) {
            try {
                // Fallback: try using systeminfo command
                val process = ProcessBuilder("cmd", "/c", "systeminfo | findstr /B /C:\"OS Name\" /C:\"OS Version\"").start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                
                val lines = reader.readLines()
                reader.close()
                process.waitFor()
                
                val osNameLine = lines.find { it.contains("OS Name") }?.substringAfter(":")?.trim()?.replace("Microsoft ", "")
                val osVersionLine = lines.find { it.contains("OS Version") }?.substringAfter(":")?.trim()
                
                when {
                    osNameLine != null && osVersionLine != null -> "$osNameLine ($osVersionLine)"
                    osVersionLine != null -> osVersionLine
                    osNameLine != null -> osNameLine
                    else -> System.getProperty("os.version") ?: "Unknown"
                }
            } catch (e2: Exception) {
                // Final fallback to system property
                System.getProperty("os.version") ?: "Unknown"
            }
        }
    }
    
    private fun getLinuxVersion(): String {
        return try {
            // Try to read from /etc/os-release first (most modern distributions)
            val osReleaseFile = File("/etc/os-release")
            if (osReleaseFile.exists()) {
                val lines = osReleaseFile.readLines()
                val prettyName = lines.find { it.startsWith("PRETTY_NAME=") }
                    ?.substringAfter("PRETTY_NAME=")
                    ?.removeSurrounding("\"")
                
                if (prettyName != null) {
                    return prettyName
                }
                
                // Fallback to NAME and VERSION_ID
                val name = lines.find { it.startsWith("NAME=") }
                    ?.substringAfter("NAME=")
                    ?.removeSurrounding("\"")
                val version = lines.find { it.startsWith("VERSION_ID=") }
                    ?.substringAfter("VERSION_ID=")
                    ?.removeSurrounding("\"")
                
                if (name != null && version != null) {
                    return "$name $version"
                } else if (name != null) {
                    return name
                }
            }
            
            // Try lsb_release command
            try {
                val process = ProcessBuilder("lsb_release", "-d").start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val line = reader.readLine()
                reader.close()
                process.waitFor()
                
                if (line != null && line.contains("Description:")) {
                    return line.substringAfter("Description:").trim()
                }
            } catch (e: Exception) {
                // lsb_release not available, continue to other methods
            }
            
            // Try reading from /etc/issue
            val issueFile = File("/etc/issue")
            if (issueFile.exists()) {
                val content = issueFile.readText().trim()
                if (content.isNotEmpty()) {
                    // Clean up the content (remove escape sequences and extra info)
                    return content.split("\\").first().trim()
                }
            }
            
            // Try uname command for kernel version
            val process = ProcessBuilder("uname", "-r").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val kernelVersion = reader.readLine() ?: ""
            reader.close()
            process.waitFor()
            
            if (kernelVersion.isNotEmpty()) {
                "Linux $kernelVersion"
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            // Final fallback to system property
            System.getProperty("os.version") ?: "Unknown"
        }
    }
    
    override fun getOSDisplayString(): String {
        return when {
            osName.contains("linux") -> getOSVersion() // For Linux, the version already includes the distribution name
            else -> "${getOSName()} ${getOSVersion()}"
        }
    }
}

/**
 * Platform-specific factory function for desktop
 */
actual fun createOSInfo(): OSInfo = DesktopOSInfo()