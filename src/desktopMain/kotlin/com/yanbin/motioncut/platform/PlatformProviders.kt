package com.yanbin.motioncut.platform

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import java.text.DecimalFormat

/**
 * Internal interface for platform-specific OS information providers
 */
internal interface PlatformOSInfoProvider {
    fun getOSName(): String
    fun getOSVersion(): String
    fun getMemoryInfo(): MemoryInfo
    fun getCPUInfo(): CPUInfo
    fun getGPUInfo(): List<GPUInfo>
}

/**
 * macOS-specific implementation
 */
internal class MacOSInfoProvider : PlatformOSInfoProvider {
    private val memoryBean = ManagementFactory.getMemoryMXBean()
    private val osBean = ManagementFactory.getOperatingSystemMXBean()
    
    override fun getOSName(): String = "macOS"
    
    override fun getOSVersion(): String {
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
    
    override fun getMemoryInfo(): MemoryInfo {
        return try {
            val process = ProcessBuilder("vm_stat").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val lines = reader.readLines()
            reader.close()
            process.waitFor()
            
            var pageSize = 4096L // Default page size
            var freePages = 0L
            var activePages = 0L
            var inactivePages = 0L
            var wiredPages = 0L
            
            lines.forEach { line ->
                when {
                    line.contains("page size of") -> {
                        val match = Regex("page size of (\\d+) bytes").find(line)
                        pageSize = match?.groupValues?.get(1)?.toLongOrNull() ?: 4096L
                    }
                    line.startsWith("Pages free:") -> {
                        freePages = line.substringAfter(":").trim().removeSuffix(".").toLongOrNull() ?: 0L
                    }
                    line.startsWith("Pages active:") -> {
                        activePages = line.substringAfter(":").trim().removeSuffix(".").toLongOrNull() ?: 0L
                    }
                    line.startsWith("Pages inactive:") -> {
                        inactivePages = line.substringAfter(":").trim().removeSuffix(".").toLongOrNull() ?: 0L
                    }
                    line.startsWith("Pages wired down:") -> {
                        wiredPages = line.substringAfter(":").trim().removeSuffix(".").toLongOrNull() ?: 0L
                    }
                }
            }
            
            val totalPages = freePages + activePages + inactivePages + wiredPages
            val usedPages = activePages + inactivePages + wiredPages
            val totalMemory = totalPages * pageSize
            val usedMemory = usedPages * pageSize
            val availableMemory = freePages * pageSize
            
            val usagePercentage = if (totalMemory > 0) (usedMemory.toDouble() / totalMemory.toDouble()) * 100 else 0.0
            
            MemoryInfo(
                totalMemory = formatBytes(totalMemory),
                availableMemory = formatBytes(availableMemory),
                usedMemory = formatBytes(usedMemory),
                usagePercentage = usagePercentage
            )
        } catch (e: Exception) {
            getJavaMemory()
        }
    }
    
    override fun getCPUInfo(): CPUInfo {
        return try {
            val nameProcess = ProcessBuilder("sysctl", "-n", "machdep.cpu.brand_string").start()
            val nameReader = BufferedReader(InputStreamReader(nameProcess.inputStream))
            val cpuName = nameReader.readLine() ?: "Unknown CPU"
            nameReader.close()
            nameProcess.waitFor()
            
            val coreProcess = ProcessBuilder("sysctl", "-n", "hw.physicalcpu").start()
            val coreReader = BufferedReader(InputStreamReader(coreProcess.inputStream))
            val cores = coreReader.readLine()?.toIntOrNull() ?: osBean.availableProcessors
            coreReader.close()
            coreProcess.waitFor()
            
            val threadProcess = ProcessBuilder("sysctl", "-n", "hw.logicalcpu").start()
            val threadReader = BufferedReader(InputStreamReader(threadProcess.inputStream))
            val threads = threadReader.readLine()?.toIntOrNull() ?: osBean.availableProcessors
            threadReader.close()
            threadProcess.waitFor()
            
            val arch = System.getProperty("os.arch") ?: "Unknown"
            
            CPUInfo(
                name = cpuName.trim(),
                cores = cores,
                threads = threads,
                architecture = arch
            )
        } catch (e: Exception) {
            getJavaCPU()
        }
    }
    
    override fun getGPUInfo(): List<GPUInfo> {
        return try {
            val process = ProcessBuilder("system_profiler", "SPDisplaysDataType", "-json").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()
            process.waitFor()
            
            // Simple parsing for GPU info (could be enhanced with JSON parsing library)
            val gpuList = mutableListOf<GPUInfo>()
            val lines = output.split("\n")
            var currentGPU: String? = null
            var currentVendor: String? = null
            var currentMemory: String? = null
            
            lines.forEach { line ->
                when {
                    line.contains("\"_name\"") -> {
                        currentGPU = line.substringAfter(":").trim().removeSurrounding("\"", "\",")
                    }
                    line.contains("\"sppci_vendor\"") -> {
                        currentVendor = line.substringAfter(":").trim().removeSurrounding("\"", "\",")
                    }
                    line.contains("\"sppci_vram\"") -> {
                        currentMemory = line.substringAfter(":").trim().removeSurrounding("\"", "\",")
                    }
                }
            }
            
            if (currentGPU != null) {
                gpuList.add(GPUInfo(
                    name = currentGPU!!,
                    vendor = currentVendor ?: "Unknown",
                    memory = currentMemory
                ))
            }
            
            gpuList
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun getJavaMemory(): MemoryInfo {
        val heapMemory = memoryBean.heapMemoryUsage
        val nonHeapMemory = memoryBean.nonHeapMemoryUsage
        
        val totalHeap = heapMemory.max
        val usedHeap = heapMemory.used
        val availableHeap = totalHeap - usedHeap
        
        val usagePercentage = if (totalHeap > 0) (usedHeap.toDouble() / totalHeap.toDouble()) * 100 else 0.0
        
        return MemoryInfo(
            totalMemory = formatBytes(totalHeap),
            availableMemory = formatBytes(availableHeap),
            usedMemory = formatBytes(usedHeap),
            usagePercentage = usagePercentage
        )
    }
    
    private fun getJavaCPU(): CPUInfo {
        val processors = osBean.availableProcessors
        val arch = System.getProperty("os.arch") ?: "Unknown"
        
        return CPUInfo(
            name = "Unknown CPU",
            cores = processors,
            threads = processors,
            architecture = arch
        )
    }
}

/**
 * Windows-specific implementation
 */
internal class WindowsOSInfoProvider : PlatformOSInfoProvider {
    private val memoryBean = ManagementFactory.getMemoryMXBean()
    private val osBean = ManagementFactory.getOperatingSystemMXBean()
    
    override fun getOSName(): String = "Windows"
    
    override fun getOSVersion(): String {
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
    
    override fun getMemoryInfo(): MemoryInfo {
        return try {
            val process = ProcessBuilder("wmic", "OS", "get", "TotalVisibleMemorySize,FreePhysicalMemory", "/format:list").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val lines = reader.readLines()
            reader.close()
            process.waitFor()
            
            var totalMemory = 0L
            var freeMemory = 0L
            
            lines.forEach { line ->
                when {
                    line.startsWith("TotalVisibleMemorySize=") -> {
                        totalMemory = line.substringAfter("=").toLongOrNull()?.times(1024) ?: 0L
                    }
                    line.startsWith("FreePhysicalMemory=") -> {
                        freeMemory = line.substringAfter("=").toLongOrNull()?.times(1024) ?: 0L
                    }
                }
            }
            
            val usedMemory = totalMemory - freeMemory
            val usagePercentage = if (totalMemory > 0) (usedMemory.toDouble() / totalMemory.toDouble()) * 100 else 0.0
            
            MemoryInfo(
                totalMemory = formatBytes(totalMemory),
                availableMemory = formatBytes(freeMemory),
                usedMemory = formatBytes(usedMemory),
                usagePercentage = usagePercentage
            )
        } catch (e: Exception) {
            getJavaMemory()
        }
    }
    
    override fun getCPUInfo(): CPUInfo {
        return try {
            val process = ProcessBuilder("wmic", "cpu", "get", "Name,NumberOfCores,NumberOfLogicalProcessors", "/format:list").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val lines = reader.readLines()
            reader.close()
            process.waitFor()
            
            var cpuName = "Unknown CPU"
            var cores = osBean.availableProcessors
            var threads = osBean.availableProcessors
            
            lines.forEach { line ->
                when {
                    line.startsWith("Name=") -> {
                        cpuName = line.substringAfter("=").trim()
                    }
                    line.startsWith("NumberOfCores=") -> {
                        cores = line.substringAfter("=").toIntOrNull() ?: cores
                    }
                    line.startsWith("NumberOfLogicalProcessors=") -> {
                        threads = line.substringAfter("=").toIntOrNull() ?: threads
                    }
                }
            }
            
            val arch = System.getProperty("os.arch") ?: "Unknown"
            
            CPUInfo(
                name = cpuName,
                cores = cores,
                threads = threads,
                architecture = arch
            )
        } catch (e: Exception) {
            getJavaCPU()
        }
    }
    
    override fun getGPUInfo(): List<GPUInfo> {
        return try {
            val process = ProcessBuilder("wmic", "path", "win32_VideoController", "get", "Name,AdapterRAM,DriverVersion", "/format:list").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val lines = reader.readLines()
            reader.close()
            process.waitFor()
            
            val gpuList = mutableListOf<GPUInfo>()
            var currentName: String? = null
            var currentMemory: String? = null
            var currentDriver: String? = null
            
            lines.forEach { line ->
                when {
                    line.startsWith("Name=") && line.substringAfter("=").isNotBlank() -> {
                        currentName = line.substringAfter("=").trim()
                    }
                    line.startsWith("AdapterRAM=") && line.substringAfter("=").isNotBlank() -> {
                        val ram = line.substringAfter("=").toLongOrNull()
                        currentMemory = if (ram != null && ram > 0) formatBytes(ram) else null
                    }
                    line.startsWith("DriverVersion=") && line.substringAfter("=").isNotBlank() -> {
                        currentDriver = line.substringAfter("=").trim()
                        
                        // When we have all info for a GPU, add it to the list
                        if (currentName != null) {
                            gpuList.add(GPUInfo(
                                name = currentName!!,
                                vendor = "Unknown",
                                memory = currentMemory,
                                driver = currentDriver
                            ))
                            currentName = null
                            currentMemory = null
                            currentDriver = null
                        }
                    }
                }
            }
            
            gpuList
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun getJavaMemory(): MemoryInfo {
        val heapMemory = memoryBean.heapMemoryUsage
        val nonHeapMemory = memoryBean.nonHeapMemoryUsage
        
        val totalHeap = heapMemory.max
        val usedHeap = heapMemory.used
        val availableHeap = totalHeap - usedHeap
        
        val usagePercentage = if (totalHeap > 0) (usedHeap.toDouble() / totalHeap.toDouble()) * 100 else 0.0
        
        return MemoryInfo(
            totalMemory = formatBytes(totalHeap),
            availableMemory = formatBytes(availableHeap),
            usedMemory = formatBytes(usedHeap),
            usagePercentage = usagePercentage
        )
    }
    
    private fun getJavaCPU(): CPUInfo {
        val processors = osBean.availableProcessors
        val arch = System.getProperty("os.arch") ?: "Unknown"
        
        return CPUInfo(
            name = "Unknown CPU",
            cores = processors,
            threads = processors,
            architecture = arch
        )
    }
}

/**
 * Linux-specific implementation
 */
internal class LinuxOSInfoProvider : PlatformOSInfoProvider {
    private val memoryBean = ManagementFactory.getMemoryMXBean()
    private val osBean = ManagementFactory.getOperatingSystemMXBean()
    
    override fun getOSName(): String = "Linux"
    
    override fun getOSVersion(): String {
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
    
    override fun getMemoryInfo(): MemoryInfo {
        return try {
            val memInfoFile = File("/proc/meminfo")
            if (memInfoFile.exists()) {
                val lines = memInfoFile.readLines()
                var totalMemory = 0L
                var availableMemory = 0L
                var freeMemory = 0L
                var buffers = 0L
                var cached = 0L
                
                lines.forEach { line ->
                    when {
                        line.startsWith("MemTotal:") -> {
                            totalMemory = line.split("\\s+".toRegex())[1].toLongOrNull()?.times(1024) ?: 0L
                        }
                        line.startsWith("MemAvailable:") -> {
                            availableMemory = line.split("\\s+".toRegex())[1].toLongOrNull()?.times(1024) ?: 0L
                        }
                        line.startsWith("MemFree:") -> {
                            freeMemory = line.split("\\s+".toRegex())[1].toLongOrNull()?.times(1024) ?: 0L
                        }
                        line.startsWith("Buffers:") -> {
                            buffers = line.split("\\s+".toRegex())[1].toLongOrNull()?.times(1024) ?: 0L
                        }
                        line.startsWith("Cached:") -> {
                            cached = line.split("\\s+".toRegex())[1].toLongOrNull()?.times(1024) ?: 0L
                        }
                    }
                }
                
                // If MemAvailable is not available, calculate it
                if (availableMemory == 0L) {
                    availableMemory = freeMemory + buffers + cached
                }
                
                val usedMemory = totalMemory - availableMemory
                val usagePercentage = if (totalMemory > 0) (usedMemory.toDouble() / totalMemory.toDouble()) * 100 else 0.0
                
                MemoryInfo(
                    totalMemory = formatBytes(totalMemory),
                    availableMemory = formatBytes(availableMemory),
                    usedMemory = formatBytes(usedMemory),
                    usagePercentage = usagePercentage
                )
            } else {
                getJavaMemory()
            }
        } catch (e: Exception) {
            getJavaMemory()
        }
    }
    
    override fun getCPUInfo(): CPUInfo {
        return try {
            val cpuInfoFile = File("/proc/cpuinfo")
            if (cpuInfoFile.exists()) {
                val lines = cpuInfoFile.readLines()
                var cpuName = "Unknown CPU"
                var cores = 0
                val processors = mutableSetOf<String>()
                
                lines.forEach { line ->
                    when {
                        line.startsWith("model name") -> {
                            cpuName = line.substringAfter(":").trim()
                        }
                        line.startsWith("cpu cores") -> {
                            cores = line.substringAfter(":").trim().toIntOrNull() ?: cores
                        }
                        line.startsWith("processor") -> {
                            processors.add(line.substringAfter(":").trim())
                        }
                    }
                }
                
                val threads = processors.size
                if (cores == 0) cores = threads
                
                val arch = System.getProperty("os.arch") ?: "Unknown"
                
                CPUInfo(
                    name = cpuName,
                    cores = cores,
                    threads = threads,
                    architecture = arch
                )
            } else {
                getJavaCPU()
            }
        } catch (e: Exception) {
            getJavaCPU()
        }
    }
    
    override fun getGPUInfo(): List<GPUInfo> {
        return try {
            val process = ProcessBuilder("lspci", "-v").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val lines = reader.readLines()
            reader.close()
            process.waitFor()
            
            val gpuList = mutableListOf<GPUInfo>()
            var currentGPU: String? = null
            var currentVendor: String? = null
            
            lines.forEach { line ->
                if (line.contains("VGA compatible controller") || line.contains("3D controller")) {
                    val parts = line.split(":")
                    if (parts.size >= 3) {
                        currentVendor = parts[1].trim()
                        currentGPU = parts[2].trim()
                        
                        gpuList.add(GPUInfo(
                            name = currentGPU ?: "Unknown GPU",
                            vendor = currentVendor ?: "Unknown"
                        ))
                    }
                }
            }
            
            gpuList
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun getJavaMemory(): MemoryInfo {
        val heapMemory = memoryBean.heapMemoryUsage
        val nonHeapMemory = memoryBean.nonHeapMemoryUsage
        
        val totalHeap = heapMemory.max
        val usedHeap = heapMemory.used
        val availableHeap = totalHeap - usedHeap
        
        val usagePercentage = if (totalHeap > 0) (usedHeap.toDouble() / totalHeap.toDouble()) * 100 else 0.0
        
        return MemoryInfo(
            totalMemory = formatBytes(totalHeap),
            availableMemory = formatBytes(availableHeap),
            usedMemory = formatBytes(usedHeap),
            usagePercentage = usagePercentage
        )
    }
    
    private fun getJavaCPU(): CPUInfo {
        val processors = osBean.availableProcessors
        val arch = System.getProperty("os.arch") ?: "Unknown"
        
        return CPUInfo(
            name = "Unknown CPU",
            cores = processors,
            threads = processors,
            architecture = arch
        )
    }
}

/**
 * Generic fallback implementation for unknown platforms
 */
internal class GenericOSInfoProvider : PlatformOSInfoProvider {
    private val memoryBean = ManagementFactory.getMemoryMXBean()
    private val osBean = ManagementFactory.getOperatingSystemMXBean()
    
    override fun getOSName(): String = System.getProperty("os.name") ?: "Unknown"
    
    override fun getOSVersion(): String = System.getProperty("os.version") ?: "Unknown"
    
    override fun getMemoryInfo(): MemoryInfo {
        val heapMemory = memoryBean.heapMemoryUsage
        val nonHeapMemory = memoryBean.nonHeapMemoryUsage
        
        val totalHeap = heapMemory.max
        val usedHeap = heapMemory.used
        val availableHeap = totalHeap - usedHeap
        
        val usagePercentage = if (totalHeap > 0) (usedHeap.toDouble() / totalHeap.toDouble()) * 100 else 0.0
        
        return MemoryInfo(
            totalMemory = formatBytes(totalHeap),
            availableMemory = formatBytes(availableHeap),
            usedMemory = formatBytes(usedHeap),
            usagePercentage = usagePercentage
        )
    }
    
    override fun getCPUInfo(): CPUInfo {
        val processors = osBean.availableProcessors
        val arch = System.getProperty("os.arch") ?: "Unknown"
        
        return CPUInfo(
            name = "Unknown CPU",
            cores = processors,
            threads = processors,
            architecture = arch
        )
    }
    
    override fun getGPUInfo(): List<GPUInfo> = emptyList()
}

/**
 * Utility function to format bytes
 */
internal fun formatBytes(bytes: Long): String {
    val df = DecimalFormat("#.##")
    return when {
        bytes >= 1024 * 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
        bytes >= 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024.0))} MB"
        bytes >= 1024 -> "${df.format(bytes / 1024.0)} KB"
        else -> "$bytes B"
    }
}