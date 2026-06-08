package com.jishiyong.agent

data class InventoryPlan(
    val action: InventoryAction,
    val diagnostics: List<InventoryPlanningDiagnostic> = emptyList()
) {
    fun withDiagnostic(diagnostic: InventoryPlanningDiagnostic): InventoryPlan {
        return copy(diagnostics = diagnostics + diagnostic)
    }
}

data class InventoryPlanningDiagnostic(
    val kind: InventoryPlanningDiagnosticKind,
    val message: String,
    val technicalMessage: String? = null
)

enum class InventoryPlanningDiagnosticKind {
    LLM_FALLBACK,
    LLM_CLARIFICATION,
    MEMORY_UNAVAILABLE
}
