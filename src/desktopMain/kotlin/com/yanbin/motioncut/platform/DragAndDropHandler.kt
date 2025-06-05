package com.yanbin.motioncut.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.window.WindowScope
import com.yanbin.motioncut.domain.VideoFile
import com.yanbin.motioncut.ui.components.createVideoFileFromPath
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import java.io.File

// Global state for drag and drop
private var globalOnFileDropped: ((VideoFile) -> Unit)? = null
private var globalOnDragStateChanged: ((Boolean) -> Unit)? = null

@Composable
actual fun DragAndDropContainer(
    onFileDropped: (VideoFile) -> Unit,
    onDragStateChanged: (Boolean) -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    // Store callbacks globally so they can be accessed by the AWT drop target
    LaunchedEffect(onFileDropped, onDragStateChanged) {
        globalOnFileDropped = onFileDropped
        globalOnDragStateChanged = onDragStateChanged
    }
    
    Box(modifier = modifier) {
        content()
    }
}

/**
 * Sets up AWT-based drag and drop for a Compose window
 * Call this from the WindowScope
 */
@Composable
fun WindowScope.setupWindowDragAndDrop() {
    LaunchedEffect(window) {
        setupDragAndDrop(window as ComposeWindow)
    }
}

/**
 * Sets up AWT-based drag and drop for a Compose window
 */
private fun setupDragAndDrop(window: ComposeWindow) {
    val dropTarget = object : DropTarget() {
        override fun dragEnter(dtde: DropTargetDragEvent) {
            globalOnDragStateChanged?.invoke(true)
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY)
            } else {
                dtde.rejectDrag()
            }
        }
        
        override fun dragOver(dtde: DropTargetDragEvent) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY)
            } else {
                dtde.rejectDrag()
            }
        }
        
        override fun dragExit(dte: DropTargetEvent) {
            globalOnDragStateChanged?.invoke(false)
        }
        
        override fun drop(dtde: DropTargetDropEvent) {
            globalOnDragStateChanged?.invoke(false)
            
            try {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY)
                    
                    val transferable = dtde.transferable
                    val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                    
                    if (files.isNotEmpty()) {
                        val file = files.first()
                        if (file.exists() && file.isFile) {
                            val videoFile = createVideoFileFromPath(file.absolutePath)
                            if (videoFile != null) {
                                globalOnFileDropped?.invoke(videoFile)
                                dtde.dropComplete(true)
                                return
                            }
                        }
                    }
                }
                dtde.dropComplete(false)
            } catch (e: Exception) {
                println("Error handling file drop: ${e.message}")
                dtde.dropComplete(false)
            }
        }
    }
    
    // Set the drop target on the window's content pane
    window.contentPane.dropTarget = dropTarget
}