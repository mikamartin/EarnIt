package com.earnit.app

import com.earnit.app.data.EarnItRepository
import com.earnit.app.data.TaskEntity
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class PointFormulaTest {
    private val repository = EarnItRepository(mockk(relaxed = true))

    @Test
    fun `auto point formula minimum values`() {
        // ceil((1+1)(1+1)(1+1)/8) + 0 = 1
        assertEquals(1, repository.computeAutoPoints(1, 1, 1))
    }

    @Test
    fun `auto point formula maximum values`() {
        // ceil((6)(6)(6)/8) + 3 = 27 + 3 = 30
        assertEquals(30, repository.computeAutoPoints(5, 5, 5))
    }

    @Test
    fun `auto point formula mixed values`() {
        // ceil((3)(6)(3)/8) + 3 = ceil(6.75) + 3 = 7 + 3 = 10
        assertEquals(10, repository.computeAutoPoints(2, 5, 2))
    }

    @Test
    fun `auto point formula medium values`() {
        // ceil((4)(4)(4)/8) + 0 = 8 + 0 = 8
        assertEquals(8, repository.computeAutoPoints(3, 3, 3))
    }

    @Test
    fun `auto point formula max single dimension bonus - time`() {
        // ceil((6)(2)(2)/8) + 3 = ceil(3) + 3 = 6
        assertEquals(6, repository.computeAutoPoints(5, 1, 1))
    }

    @Test
    fun `auto point formula max single dimension bonus - difficulty`() {
        // ceil((2)(6)(2)/8) + 3 = ceil(3) + 3 = 6
        assertEquals(6, repository.computeAutoPoints(1, 5, 1))
    }

    @Test
    fun `auto point formula max single dimension bonus - preparation`() {
        // ceil((2)(2)(6)/8) + 3 = ceil(3) + 3 = 6
        assertEquals(6, repository.computeAutoPoints(1, 1, 5))
    }

    @Test
    fun `TaskEntity effectivePoints uses auto formula when flag set`() {
        val task = TaskEntity(name = "test", useAutoPoints = true, time = 3, difficulty = 3, preparation = 3)
        assertEquals(8, task.effectivePoints())
    }

    @Test
    fun `TaskEntity effectivePoints uses manual points when flag unset`() {
        val task =
            TaskEntity(name = "test", useAutoPoints = false, points = 7, time = 5, difficulty = 5, preparation = 5)
        assertEquals(7, task.effectivePoints())
    }
}
