package com.jishiyong.agent

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceInputStateTest {

    @Test
    fun parsingStateDefaultsToLocalRulesMode() {
        val state = VoiceInputState.Parsing("喝一瓶牛奶")

        assertEquals("喝一瓶牛奶", state.recognizedText)
        assertEquals(InventoryAgentMode.LOCAL_RULES.displayName, state.parserLabel)
        assertEquals(InventoryAgentMode.LOCAL_RULES.parsingMessagePrefix, state.messagePrefix)
    }
}
