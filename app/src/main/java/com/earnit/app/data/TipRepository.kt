package com.earnit.app.data

import android.app.Activity
import kotlinx.coroutines.delay

data class TipOption(
    val productId: String,
    val title: String,
    val formattedPrice: String,
)

sealed class PurchaseResult {
    object Success : PurchaseResult()

    object Cancelled : PurchaseResult()

    data class Error(
        val message: String,
    ) : PurchaseResult()
}

interface TipRepository {
    suspend fun fetchTipOptions(): List<TipOption>

    suspend fun purchase(
        activity: Activity,
        productId: String,
    ): PurchaseResult
}

// Swapped out when RevenueCat is integrated — see Tip Jar section in EARNIT_SPEC.md
class MockTipRepository : TipRepository {
    override suspend fun fetchTipOptions(): List<TipOption> {
        delay(300)
        return listOf(
            TipOption("tip_small", "Tiny Tip", "$2.99"),
            TipOption("tip_coffee", "Coffee Tip", "$5.99"),
        )
    }

    override suspend fun purchase(
        activity: Activity,
        productId: String,
    ): PurchaseResult {
        delay(800)
        return PurchaseResult.Success
    }
}
