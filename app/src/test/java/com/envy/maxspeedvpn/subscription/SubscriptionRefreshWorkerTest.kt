package com.envy.maxspeedvpn.subscription

import androidx.work.ExistingPeriodicWorkPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionRefreshWorkerTest {
    @Test
    fun `rescheduling keeps the existing periodic interval instead of postponing it`() {
        assertEquals(ExistingPeriodicWorkPolicy.KEEP, subscriptionRefreshWorkPolicy())
    }
}
