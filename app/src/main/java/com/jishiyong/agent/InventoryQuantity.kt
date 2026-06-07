package com.jishiyong.agent

import com.jishiyong.data.db.entity.Item

fun Item.remainingQuantity(): Int = (quantity - usedQuantity).coerceAtLeast(0)
