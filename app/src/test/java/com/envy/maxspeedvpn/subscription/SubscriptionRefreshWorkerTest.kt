package com.envy.maxspeedvpn.subscription

import androidx.work.ExistingPeriodicWorkPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionRefreshWorkerTest {
    @Test
    fun `rescheduling updates legacy periodic interval in place`() {
        assertEquals(ExistingPeriodicWorkPolicy.UPDATE, subscriptionRefreshWorkPolicy(intervalMigrationPending = true))
    }

    @Test
    fun `routine scheduling keeps migrated work without postponing it`() {
        assertEquals(ExistingPeriodicWorkPolicy.KEEP, subscriptionRefreshWorkPolicy(intervalMigrationPending = false))
    }

    @Test
    fun `automatic subscription refresh defaults to one hour`() {
        assertEquals(1L, subscriptionRefreshIntervalHours())
    }
}
