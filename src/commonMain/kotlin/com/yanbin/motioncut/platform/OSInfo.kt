package com.yanbin.motioncut.platform

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
     * Get a formatted string with OS name and version
     */
    fun getOSDisplayString(): String = "${getOSName()} ${getOSVersion()}"
}

/**
 * Expected declaration for platform-specific implementation
 */
expect fun createOSInfo(): OSInfo