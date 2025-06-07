package com.yanbin.motioncut.services

/**
 * Common interface for video trimming operations
 */
interface VideoTrimmerService {
    /**
     * Trim video to half its original length
     * @param inputPath Path to the input video file
     * @param outputPath Path where the trimmed video will be saved
     * @param onProgress Callback for progress updates (0.0 to 1.0)
     * @param onComplete Callback when trimming is complete
     * @param onError Callback when an error occurs
     */
    suspend fun trimVideoToHalf(
        inputPath: String,
        outputPath: String,
        onProgress: (Float) -> Unit = {},
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    )
    
    /**
     * Generate output path for trimmed video
     */
    fun generateOutputPath(inputPath: String): String
}

/**
 * Platform-specific video trimmer service factory
 */
expect fun createVideoTrimmerService(): VideoTrimmerService