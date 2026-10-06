package com.contextguard.app

import com.contextguard.app.core.network.BackendConfig
import com.contextguard.app.core.network.InferenceMode
import com.contextguard.app.ui.viewmodel.InterventionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyDomainUnitTest {

    @Test
    fun testBackendConfigDefaultBaseUrl() {
        val config = BackendConfig(host = "10.0.2.2", port = 8000)
        assertEquals("http://10.0.2.2:8000", config.baseUrl)
        assertEquals(InferenceMode.REDACTED_LOCAL_BACKEND, config.inferenceMode)
    }

    @Test
    fun testCentralDemoPolicyCalculations() {
        val lambda = 0.75f

        // Action 1: Save Privately (s=0.05, r=0.0) -> rho = 0.05 -> ACT
        val s1 = 0.05f
        val r1 = 0.00f
        val rho1 = s1 * (1.0f + lambda * r1)
        val intervention1 = if (rho1 >= 0.65f) InterventionType.STOP else if (rho1 >= 0.35f) InterventionType.WARN else InterventionType.ACT
        assertEquals(InterventionType.ACT, intervention1)

        // Action 3: Post Publicly (s=0.90, r=1.0) -> rho = 1.575 -> STOP
        val s3 = 0.90f
        val r3 = 1.00f
        val rho3 = s3 * (1.0f + lambda * r3)
        assertTrue("Risk score should exceed STOP threshold", rho3 >= 0.65f)
        val intervention3 = if (rho3 >= 0.65f) InterventionType.STOP else if (rho3 >= 0.35f) InterventionType.WARN else InterventionType.ACT
        assertEquals(InterventionType.STOP, intervention3)
    }
}
