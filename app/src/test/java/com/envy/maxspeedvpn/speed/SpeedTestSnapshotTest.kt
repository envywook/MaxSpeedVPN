package com.envy.maxspeedvpn.speed

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedTestSnapshotTest {
    @Test
    fun `completion reports the average of download and upload`() {
        val completed = SpeedTestSnapshot.complete(downloadMbps = 120.0, uploadMbps = 80.0)

        assertEquals(SpeedTestPhase.COMPLETE, completed.phase)
        assertEquals(100.0, completed.megabitsPerSecond, 0.001)
        assertEquals(120.0, completed.downloadMbps!!, 0.001)
        assertEquals(80.0, completed.uploadMbps!!, 0.001)
    }
}
