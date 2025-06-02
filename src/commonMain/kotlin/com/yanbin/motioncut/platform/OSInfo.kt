package com.yanbin.motioncut.platform

/**
 * Data class for system memory information
 */
data class MemoryInfo(
    val totalMemory: String,
    val availableMemory: String,
    val usedMemory: String,
    val usagePercentage: Double
)

/**
 * Data class for CPU information
 */
data class CPUInfo(
    val name: String,
    val cores: Int,
    val threads: Int,
    val architecture: String,
    val frequency: String? = null
)

/**
 * Data class for GPU information
 */
data class GPUInfo(
    val name: String,
    val vendor: String,
    val memory: String? = null,
    val driver: String? = null
)

/**
 * Interface for getting OS-specific information
 */
interface OSInfo {
    /**
     * Get the operating system name
     */
    fun getOSName(): String
    
    /**
     * Get the operating system version
     */
    fun getOSVersion(): String
    
    /**
     * Get memory information
     */
    fun getMemoryInfo(): MemoryInfo
    
    /**
     * Get CPU information
     */
    fun getCPUInfo(): CPUInfo
    
    /**
     * Get GPU information
     */
    fun getGPUInfo(): List<GPUInfo>
    
    /**
     * Get a formatted string with OS name and version
     */
    fun getOSDisplayString(): String = "${getOSName()} ${getOSVersion()}"
}

/**
 * Expected declaration for platform-specific implementation
 */
expect fun createOSInfo(): OSInfo