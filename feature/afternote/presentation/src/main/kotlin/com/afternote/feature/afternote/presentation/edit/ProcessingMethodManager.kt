package com.afternote.feature.afternote.presentation.edit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import com.kuit.afternote.feature.afternote.presentation.edit.model.ProcessingMethodItem
import kotlin.String
import kotlin.collections.List
import kotlin.collections.copy
import kotlin.collections.emptyList
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.collections.plus
import kotlin.map
import kotlin.sequences.filter
import kotlin.sequences.map
import kotlin.text.filter
import kotlin.text.map
import kotlin.toString

/**
 * Holds processing method list state and actions for the afternote edit screen.
 * Extracted from [com.kuit.afternote.feature.afternote.presentation.edit.AfternoteEditState] to keep function count under the detekt threshold.
 */
@Stable
class ProcessingMethodManager {
    var processingMethods by mutableStateOf<List<ProcessingMethodItem>>(
        emptyList(),
    )
        private set
    var galleryProcessingMethods by mutableStateOf<List<ProcessingMethodItem>>(
        emptyList(),
    )
        private set

    fun replaceProcessingMethods(list: List<ProcessingMethodItem>) {
        processingMethods = list
    }

    fun replaceGalleryProcessingMethods(list: List<ProcessingMethodItem>) {
        galleryProcessingMethods = list
    }

    fun editProcessingMethod(
        itemId: String,
        newText: String,
    ) {
        processingMethods =
            processingMethods.map { item ->
                if (item.id == itemId) item.copy(text = newText) else item
            }
    }

    fun deleteProcessingMethod(itemId: String) {
        processingMethods = processingMethods.filter { it.id != itemId }
    }

    fun addProcessingMethod(text: String) {
        val newItem =
            ProcessingMethodItem(
                id = (processingMethods.size + 1).toString(),
                text = text,
            )
        processingMethods = processingMethods + newItem
    }

    fun editGalleryProcessingMethod(
        itemId: String,
        newText: String,
    ) {
        galleryProcessingMethods =
            galleryProcessingMethods.map { item ->
                if (item.id == itemId) item.copy(text = newText) else item
            }
    }

    fun deleteGalleryProcessingMethod(itemId: String) {
        galleryProcessingMethods = galleryProcessingMethods.filter { it.id != itemId }
    }

    fun addGalleryProcessingMethod(text: String) {
        val newItem =
            ProcessingMethodItem(
                id = (galleryProcessingMethods.size + 1).toString(),
                text = text,
            )
        galleryProcessingMethods = galleryProcessingMethods + newItem
    }
}
