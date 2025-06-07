package com.yanbin.motioncut.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.javacv.FFmpegFrameGrabber
import java.io.File

/**
 * Desktop video trimming service using JavaCV
 */
class VideoTrimmerService {
    
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
    ) {
        withContext(Dispatchers.IO) {
            try {
                // First, get video duration using FFmpegFrameGrabber
                val grabber = FFmpegFrameGrabber(inputPath).apply { start() }
                val totalDurationSeconds = grabber.lengthInTime / 1_000_000.0
                val halfDurationSeconds = totalDurationSeconds / 2.0
                grabber.stop()
                grabber.release()
                
                // Validate output path
                val outputFile = File(outputPath)
                outputFile.parentFile?.mkdirs() // Ensure directory exists
                
                // Use FFmpeg command line for better rotation and quality handling
                val ffmpegCommand = listOf(
                    "ffmpeg",
                    "-i", inputPath,
                    "-t", halfDurationSeconds.toString(), // Trim to half duration
                    "-c:v", "libx264", // H.264 video codec
                    "-preset", "medium", // Encoding speed vs compression
                    "-crf", "18", // High quality (lower = better)
                    "-c:a", "aac", // AAC audio codec
                    "-movflags", "+faststart", // Enable fast start
                    "-avoid_negative_ts", "make_zero", // Fix timestamp issues
                    "-map_metadata", "0", // Copy all metadata including rotation
                    "-y", // Overwrite output file
                    outputPath
                )
                
                // Execute FFmpeg command
                val processBuilder = ProcessBuilder(ffmpegCommand)
                processBuilder.redirectErrorStream(true)
                val process = processBuilder.start()
                
                // Monitor progress by reading FFmpeg output
                val reader = process.inputStream.bufferedReader()
                var line: String?
                var lastProgress = 0f
                
                while (reader.readLine().also { line = it } != null) {
                    line?.let { output ->
                        // Parse FFmpeg progress output
                        if (output.contains("time=")) {
                            try {
                                val timeMatch = Regex("time=(\\d+):(\\d+):(\\d+\\.\\d+)").find(output)
                                if (timeMatch != null) {
                                    val hours = timeMatch.groupValues[1].toDouble()
                                    val minutes = timeMatch.groupValues[2].toDouble()
                                    val seconds = timeMatch.groupValues[3].toDouble()
                                    val currentSeconds = hours * 3600 + minutes * 60 + seconds
                                    
                                    val progress = (currentSeconds / halfDurationSeconds).coerceIn(0.0, 1.0).toFloat()
                                    if (progress > lastProgress) {
                                        lastProgress = progress
                                        withContext(Dispatchers.Main) {
                                            onProgress(progress)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Ignore parsing errors
                            }
                        }
                    }
                }
                
                val exitCode = process.waitFor()
                
                if (exitCode == 0 && outputFile.exists()) {
                    withContext(Dispatchers.Main) {
                        onComplete()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("FFmpeg failed with exit code: $exitCode")
                    }
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Failed to trim video: ${e.message}")
                }
            }
        }
    }
}