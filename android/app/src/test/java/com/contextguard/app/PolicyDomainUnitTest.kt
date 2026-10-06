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

    @Test
    fun testReadyDemoScenariosValidation() {
        val scenarios = com.contextguard.app.ui.viewmodel.ReadyDemoScenariosList
        assertEquals(8, scenarios.size)

        // Scenario 1: Bank statement -> STOP
        assertEquals(1, scenarios[0].number)
        assertEquals("Bank statement", scenarios[0].title)
        assertEquals(InterventionType.STOP, scenarios[0].expectedIntervention)

        // Scenario 2: Fake KYC link -> STOP
        assertEquals(2, scenarios[1].number)
        assertEquals("Fake KYC link", scenarios[1].title)
        assertEquals(InterventionType.STOP, scenarios[1].expectedIntervention)

        // Scenario 3: OTP screenshot -> STOP
        assertEquals(3, scenarios[2].number)
        assertEquals("OTP screenshot", scenarios[2].title)
        assertEquals(InterventionType.STOP, scenarios[2].expectedIntervention)

        // Scenario 4: Unknown recipient document -> ASK
        assertEquals(4, scenarios[3].number)
        assertEquals("Unknown recipient document", scenarios[3].title)
        assertEquals(InterventionType.ASK, scenarios[3].expectedIntervention)

        // Scenario 5: Routine family photo -> ACT
        assertEquals(5, scenarios[4].number)
        assertEquals("Routine family photo", scenarios[4].title)
        assertEquals(InterventionType.ACT, scenarios[4].expectedIntervention)

        // Scenario 6: Routine news URL -> ACT
        assertEquals(6, scenarios[5].number)
        assertEquals("Routine news URL", scenarios[5].title)
        assertEquals(InterventionType.ACT, scenarios[5].expectedIntervention)

        // Scenario 7: Contract lock-in -> WARN
        assertEquals(7, scenarios[6].number)
        assertEquals("Contract lock-in", scenarios[6].title)
        assertEquals(InterventionType.WARN, scenarios[6].expectedIntervention)

        // Scenario 8: Payment request -> WARN
        assertEquals(8, scenarios[7].number)
        assertEquals("Payment request", scenarios[7].title)
        assertEquals(InterventionType.WARN, scenarios[7].expectedIntervention)
    }
}
