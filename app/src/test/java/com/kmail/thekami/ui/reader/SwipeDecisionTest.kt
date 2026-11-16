package com.kmail.thekami.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwipeDecisionTest {
    @Test
    fun longHorizontalSwipesChangeTheMessage() {
        assertEquals(SwipeDirection.NEXT, SwipeDecision.decide(-300f, 20f, 200f))
        assertEquals(SwipeDirection.PREVIOUS, SwipeDecision.decide(300f, -20f, 200f))
    }

    @Test
    fun shortOrDiagonalOrVerticalMovesDoNothing() {
        assertNull(SwipeDecision.decide(-100f, 0f, 200f))
        assertNull(SwipeDecision.decide(-300f, 200f, 200f))
        assertNull(SwipeDecision.decide(10f, 600f, 200f))
        assertNull(SwipeDecision.decide(0f, 0f, 200f))
    }
}
