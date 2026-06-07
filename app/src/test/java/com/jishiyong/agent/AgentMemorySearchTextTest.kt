package com.jishiyong.agent

import org.junit.Assert.assertTrue
import org.junit.Test

class AgentMemorySearchTextTest {

    @Test
    fun searchableTextIncludesAliasNgrams() {
        val text = AgentMemorySearchText.searchableTextFor(
            InventoryAliasMemory(
                alias = "常买的奶",
                canonicalName = "蒙牛纯牛奶"
            )
        )

        assertTrue(text.contains("常买"))
        assertTrue(text.contains("买奶") || text.contains("的奶"))
        assertTrue(text.contains("蒙牛"))
    }

    @Test
    fun ftsQueryIncludesInputNgrams() {
        val query = AgentMemorySearchText.ftsQueryFor("我喝了一瓶常买的奶")

        assertTrue(query.contains("\"常买\""))
        assertTrue(query.contains(" OR "))
    }
}
