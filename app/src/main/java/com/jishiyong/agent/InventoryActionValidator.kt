package com.jishiyong.agent

class InventoryActionValidator {

    fun validate(draft: ItemDraft): String? {
        return when {
            draft.name.isBlank() -> "请确认要新增的物品名称"
            draft.quantity <= 0 -> "数量必须大于 0"
            draft.expirationDate.isBefore(draft.purchaseDate) -> "过期日期不能早于购买日期"
            draft.reminderDays.any { it <= 0 } -> "提醒天数必须大于 0"
            else -> null
        }
    }
}
