package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.calculators.CulinaryCalculators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context matches Atelier Gastronomia 24S`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Atelier Gastronomia 24S", appName)
    }

    @Test
    fun `fachrechnen cascade losses and sous-vide brine math are accurate`() {
        // 180g net portion with 20% trim loss and 15% cooking loss -> 180 / (0.8 * 0.85) = 264.7058g
        val cascade = CulinaryCalculators.calculateCascadeLosses(180.0, listOf(20.0, 15.0))
        assertEquals(0.68, cascade.combinedYieldFactor, 0.001)
        assertEquals(264.71, cascade.grossPurchaseGrams, 0.05)

        // 7% Sous-vide brine on 2.0 L water -> 140g salt, fish = 10 min
        val brine = CulinaryCalculators.calculateSousVideBrine(
            waterLiters = 2.0,
            brineConcentrationPercent = 7.0,
            isDelicateFish = true,
            targetCoreTempC = 55.0,
            bathOffsetC = 0.8
        )
        assertEquals(140.0, brine.saltGramsRequired, 0.01)
        assertEquals(10, brine.recommendedBrineMinutes)
        assertEquals(55.8, brine.recommendedBathTempC, 0.01)

        // Swiss puff pastry: Deutscher (4 double folds = 256 layers) vs Halbblätterteig (3 double folds = 64 layers)
        val fullPuff = CulinaryCalculators.simulateSwissPuffPastry(1000.0, isHalfPuffHalbblaetterteig = false)
        val halfPuff = CulinaryCalculators.simulateSwissPuffPastry(1000.0, isHalfPuffHalbblaetterteig = true)
        assertEquals(4, fullPuff.recommendedDoubleFolds)
        assertEquals(256, fullPuff.theoreticalFatLayers)
        assertEquals(3, halfPuff.recommendedDoubleFolds)
        assertEquals(64, halfPuff.theoreticalFatLayers)
        assertTrue(fullPuff.storageWarning.contains("5 °C"))
    }
}
