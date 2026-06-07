package com.jishiyong.agent

import com.jishiyong.data.db.entity.Item

data class VoiceCommandContext(
    val recognizedText: String,
    val diagnostics: List<InventoryPlanningDiagnostic> = emptyList()
)

sealed class VoiceInputState {
    data object Idle : VoiceInputState()
    data object Listening : VoiceInputState()
    data object Recognizing : VoiceInputState()
    data class Parsing(
        val recognizedText: String,
        val parserLabel: String = InventoryAgentMode.LOCAL_RULES.displayName,
        val messagePrefix: String = InventoryAgentMode.LOCAL_RULES.parsingMessagePrefix
    ) : VoiceInputState()

    data class PendingConfirmation(
        val context: VoiceCommandContext,
        val confirmation: InventoryActionConfirmation
    ) : VoiceInputState() {
        constructor(
            recognizedText: String,
            action: InventoryAction,
            matchedItem: Item? = null,
            diagnostics: List<InventoryPlanningDiagnostic> = emptyList()
        ) : this(
            context = VoiceCommandContext(recognizedText, diagnostics),
            confirmation = InventoryActionConfirmation(action, matchedItem)
        )

        val recognizedText: String get() = context.recognizedText
        val action: InventoryAction get() = confirmation.action
        val matchedItem: Item? get() = confirmation.matchedItem
        val diagnostics: List<InventoryPlanningDiagnostic> get() = context.diagnostics
    }

    data class Executing(val recognizedText: String) : VoiceInputState()

    data class NeedsSelection(
        val context: VoiceCommandContext,
        val selection: InventoryCandidateSelection
    ) : VoiceInputState() {
        constructor(
            recognizedText: String,
            action: InventoryAction,
            candidates: List<Item>,
            message: String,
            diagnostics: List<InventoryPlanningDiagnostic> = emptyList()
        ) : this(
            context = VoiceCommandContext(recognizedText, diagnostics),
            selection = InventoryCandidateSelection(action, candidates, message)
        )

        val recognizedText: String get() = context.recognizedText
        val action: InventoryAction get() = selection.action
        val candidates: List<Item> get() = selection.candidates
        val message: String get() = selection.message
        val diagnostics: List<InventoryPlanningDiagnostic> get() = context.diagnostics
    }

    data class Success(val message: String) : VoiceInputState()

    data class Error(
        val message: String,
        val recognizedText: String? = null
    ) : VoiceInputState()
}
