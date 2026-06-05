package com.jishiyong.agent

import com.jishiyong.data.db.entity.Item

sealed class VoiceInputState {
    data object Idle : VoiceInputState()
    data object Listening : VoiceInputState()
    data object Recognizing : VoiceInputState()
    data class Parsing(val recognizedText: String) : VoiceInputState()

    data class PendingConfirmation(
        val recognizedText: String,
        val action: InventoryAction,
        val matchedItem: Item? = null
    ) : VoiceInputState()

    data class NeedsSelection(
        val recognizedText: String,
        val action: InventoryAction,
        val candidates: List<Item>,
        val message: String
    ) : VoiceInputState()

    data class Success(val message: String) : VoiceInputState()

    data class Error(
        val message: String,
        val recognizedText: String? = null
    ) : VoiceInputState()
}
