package com.yanbin.motioncut.platform

/**
 * Desktop implementation of OSInfo that detects the platform at runtime
 * and delegates to appropriate platform-specific implementations
 */
class DesktopOSInfo : OSInfo {
    private val osName = System.getProperty("os.name").lowercase()
    private val platformProvider: PlatformOSInfoProvider = when {
        osName.contains("mac") || osName.contains("darwin") -> MacOSInfoProvider()
        osName.contains("win") -> WindowsOSInfoProvider()
        osName.contains("linux") -> LinuxOSInfoProvider()
        else -> GenericOSInfoProvider()
    }
    
    override fun getOSName(): String = platformProvider.getOSName()
    
    override fun getOSVersion(): String = platformProvider.getOSVersion()
    
    override fun getMemoryInfo(): MemoryInfo = platformProvider.getMemoryInfo()
    
    override fun getCPUInfo(): CPUInfo = platformProvider.getCPUInfo()
    
    override fun getGPUInfo(): List<GPUInfo> = platformProvider.getGPUInfo()
    
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