package com.yanbin.motioncut.ui.video

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentSkipListMap
import kotlin.collections.set

/**
 * Custom frame buffer that manages video frames with timestamp-based access
 * Features:
 * - Efficient timestamp-based frame retrieval using TreeMap
 * - Automatic capacity management with LRU-style removal
 * - Thread-safe operations
 * - Time range tracking for available frames
 */
class TimestampFrameBuffer(private val defaultCapacity: Int = 200) {
    // TreeMap for efficient timestamp-based access (sorted by timestamp)
    private val frames = ConcurrentSkipListMap<Long, Frame>()
    private val mutex = Mutex()

    // Track time range of available frames
    @Volatile
    private var startTime: Long = Long.MAX_VALUE
    @Volatile
    private var endTime: Long = Long.MIN_VALUE

    /**
     * Get the start time of available frames
     */
    val startTimeOfAvailableFrames: Long
        get() = if (frames.isEmpty()) 0L else startTime

    /**
     * Get the end time of available frames
     */
    val endTimeOfAvailableFrames: Long
        get() = if (frames.isEmpty()) 0L else endTime

    /**
     * Get current number of frames in buffer
     */
    val size: Int
        get() = frames.size

    /**
     * Check if buffer is empty
     */
    val isEmpty: Boolean
        get() = frames.isEmpty()

    /**
     * Append a new frame to the buffer
     * If capacity is exceeded, removes the oldest frames
     */
    suspend fun appendFrame(frame: Frame) {
        mutex.withLock {
            // Add the new frame
            frames[frame.timestamp] = frame

            // Update time range
            if (frame.timestamp < startTime) {
                startTime = frame.timestamp
            }
            if (frame.timestamp > endTime) {
                endTime = frame.timestamp
            }

            // Remove oldest frames if capacity exceeded
            while (frames.size > defaultCapacity) {
                val oldestEntry = frames.firstEntry()
                if (oldestEntry != null) {
                    frames.remove(oldestEntry.key)

                    // Update start time if we removed the oldest frame
                    if (oldestEntry.key == startTime) {
                        startTime = frames.firstKey() ?: Long.MAX_VALUE
                    }
                }
            }
        }
    }

    /**
     * Check if appending a frame would cause removal of a frame at or after the given timestamp
     * Returns true if the frame would be removed
     */
    suspend fun wouldRemoveFrameAtOrAfter(protectedTimestamp: Long): Boolean {
        return mutex.withLock {
            if (frames.size < defaultCapacity) {
                false // Buffer not full, no removal needed
            } else {
                // Check if the oldest frame that would be removed is at or after the protected timestamp
                val oldestEntry = frames.firstEntry()
                oldestEntry != null && oldestEntry.key >= protectedTimestamp
            }
        }
    }

    /**
     * Get frame by exact timestamp
     * Returns null if no frame exists at the exact timestamp
     */
    suspend fun getFrame(timestamp: Long): Frame? {
        return mutex.withLock {
            frames[timestamp]
        }
    }

    fun resetBuffer() {
        frames.clear()
    }

    /**
     * Get the closest frame to the given timestamp
     * Returns null if buffer is empty or no frame within 0.033 seconds threshold
     */
    suspend fun getClosestFrame(timestamp: Long): Frame? {
        return mutex.withLock {
            if (frames.isEmpty()) return null

            // Try exact match first
            frames[timestamp]?.let { return it }

            // Find closest frame
            val lowerEntry = frames.lowerEntry(timestamp)
            val higherEntry = frames.higherEntry(timestamp)

            val thresholdMillis = 33L // 0.033 seconds in microseconds

            when {
                lowerEntry == null -> {
                    higherEntry?.let { entry ->
                        val diff = entry.key - timestamp
                        if (diff <= thresholdMillis) entry.value else null
                    }
                }
                higherEntry == null -> {
                    val diff = timestamp - lowerEntry.key
                    if (diff <= thresholdMillis) lowerEntry.value else null
                }
                else -> {
                    val lowerDiff = timestamp - lowerEntry.key
                    val higherDiff = higherEntry.key - timestamp
                    
                    // Check if closest frame is within threshold
                    val closestEntry = if (lowerDiff <= higherDiff) lowerEntry else higherEntry
                    val closestDiff = if (lowerDiff <= higherDiff) lowerDiff else higherDiff
                    
                    if (closestDiff <= thresholdMillis) closestEntry.value else null
                }
            }
        }
    }

    /**
     * Get the next available frame after the given timestamp
     * Returns null if no frame within 0.033 seconds threshold
     */
    suspend fun getNextFrame(timestamp: Long): Frame? {
        return mutex.withLock {
            val thresholdMillis = 33L
            frames.higherEntry(timestamp)?.let { entry ->
                val diff = entry.key - timestamp
                if (diff <= thresholdMillis) entry.value else null
            }
        }
    }

    /**
     * Clear all frames from the buffer
     */
    suspend fun clear() {
        mutex.withLock {
            frames.clear()
            startTime = Long.MAX_VALUE
            endTime = Long.MIN_VALUE
        }
    }

    /**
     * Get all timestamps currently in the buffer (for debugging)
     */
    suspend fun getAllTimestamps(): List<Long> {
        return mutex.withLock {
            frames.keys.toList()
        }
    }
}


/**
 * Represents a single video frame with its associated timestamp
 */
data class Frame(
    val imageBitmap: ImageBitmap,
    val timestamp: Long
)