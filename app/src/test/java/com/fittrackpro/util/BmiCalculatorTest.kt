package com.fittrackpro.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for BMI Calculator
 * Tests all BMI calculation and categorization logic
 */
class BmiCalculatorTest {

    // ==================== BMI CALCULATION TESTS ====================

    @Test
    fun `calculate BMI with normal values`() {
        // 70kg, 175cm = BMI 22.86
        val bmi = BmiCalculator.calculate(70f, 175f)
        assertEquals(22.86f, bmi, 0.1f)
    }

    @Test
    fun `calculate BMI with underweight values`() {
        // 50kg, 175cm = BMI 16.33
        val bmi = BmiCalculator.calculate(50f, 175f)
        assertEquals(16.33f, bmi, 0.1f)
    }

    @Test
    fun `calculate BMI with overweight values`() {
        // 85kg, 175cm = BMI 27.76
        val bmi = BmiCalculator.calculate(85f, 175f)
        assertEquals(27.76f, bmi, 0.1f)
    }

    @Test
    fun `calculate BMI with obese values`() {
        // 100kg, 170cm = BMI 34.6
        val bmi = BmiCalculator.calculate(100f, 170f)
        assertEquals(34.60f, bmi, 0.1f)
    }

    @Test
    fun `calculate BMI returns zero for zero weight`() {
        val bmi = BmiCalculator.calculate(0f, 175f)
        assertEquals(0f, bmi, 0.01f)
    }

    @Test
    fun `calculate BMI returns zero for zero height`() {
        val bmi = BmiCalculator.calculate(70f, 0f)
        assertEquals(0f, bmi, 0.01f)
    }

    @Test
    fun `calculate BMI returns zero for negative values`() {
        val bmi1 = BmiCalculator.calculate(-70f, 175f)
        val bmi2 = BmiCalculator.calculate(70f, -175f)
        assertEquals(0f, bmi1, 0.01f)
        assertEquals(0f, bmi2, 0.01f)
    }

    // ==================== BMI CATEGORY TESTS ====================

    @Test
    fun `getCategory returns UNDERWEIGHT for BMI below 18_5`() {
        val category = BmiCalculator.getCategory(16.5f)
        assertEquals(BmiCalculator.BmiCategory.UNDERWEIGHT, category)
    }

    @Test
    fun `getCategory returns UNDERWEIGHT for BMI at boundary 18_4`() {
        val category = BmiCalculator.getCategory(18.4f)
        assertEquals(BmiCalculator.BmiCategory.UNDERWEIGHT, category)
    }

    @Test
    fun `getCategory returns NORMAL for BMI 18_5`() {
        val category = BmiCalculator.getCategory(18.5f)
        assertEquals(BmiCalculator.BmiCategory.NORMAL, category)
    }

    @Test
    fun `getCategory returns NORMAL for BMI 22`() {
        val category = BmiCalculator.getCategory(22f)
        assertEquals(BmiCalculator.BmiCategory.NORMAL, category)
    }

    @Test
    fun `getCategory returns NORMAL for BMI 24_9`() {
        val category = BmiCalculator.getCategory(24.9f)
        assertEquals(BmiCalculator.BmiCategory.NORMAL, category)
    }

    @Test
    fun `getCategory returns OVERWEIGHT for BMI 25`() {
        val category = BmiCalculator.getCategory(25f)
        assertEquals(BmiCalculator.BmiCategory.OVERWEIGHT, category)
    }

    @Test
    fun `getCategory returns OVERWEIGHT for BMI 27`() {
        val category = BmiCalculator.getCategory(27f)
        assertEquals(BmiCalculator.BmiCategory.OVERWEIGHT, category)
    }

    @Test
    fun `getCategory returns OVERWEIGHT for BMI 29_9`() {
        val category = BmiCalculator.getCategory(29.9f)
        assertEquals(BmiCalculator.BmiCategory.OVERWEIGHT, category)
    }

    @Test
    fun `getCategory returns OBESE for BMI 30`() {
        val category = BmiCalculator.getCategory(30f)
        assertEquals(BmiCalculator.BmiCategory.OBESE, category)
    }

    @Test
    fun `getCategory returns OBESE for BMI 35`() {
        val category = BmiCalculator.getCategory(35f)
        assertEquals(BmiCalculator.BmiCategory.OBESE, category)
    }

    @Test
    fun `getCategory returns UNKNOWN for zero BMI`() {
        val category = BmiCalculator.getCategory(0f)
        assertEquals(BmiCalculator.BmiCategory.UNKNOWN, category)
    }

    @Test
    fun `getCategory returns UNKNOWN for negative BMI`() {
        val category = BmiCalculator.getCategory(-5f)
        assertEquals(BmiCalculator.BmiCategory.UNKNOWN, category)
    }

    // ==================== RECOMMENDATION TESTS ====================

    @Test
    fun `getRecommendation returns correct advice for UNDERWEIGHT`() {
        val recommendation = BmiCalculator.getRecommendation(BmiCalculator.BmiCategory.UNDERWEIGHT)
        assertTrue(recommendation.contains("calorie"))
    }

    @Test
    fun `getRecommendation returns correct advice for NORMAL`() {
        val recommendation = BmiCalculator.getRecommendation(BmiCalculator.BmiCategory.NORMAL)
        assertTrue(recommendation.contains("maintain") || recommendation.contains("Great"))
    }

    @Test
    fun `getRecommendation returns correct advice for OVERWEIGHT`() {
        val recommendation = BmiCalculator.getRecommendation(BmiCalculator.BmiCategory.OVERWEIGHT)
        assertTrue(recommendation.contains("exercise") || recommendation.contains("diet"))
    }

    @Test
    fun `getRecommendation returns correct advice for OBESE`() {
        val recommendation = BmiCalculator.getRecommendation(BmiCalculator.BmiCategory.OBESE)
        assertTrue(recommendation.contains("healthcare") || recommendation.contains("Consult"))
    }

    @Test
    fun `getRecommendation returns prompt for UNKNOWN`() {
        val recommendation = BmiCalculator.getRecommendation(BmiCalculator.BmiCategory.UNKNOWN)
        assertTrue(recommendation.contains("Enter"))
    }

    // ==================== IDEAL WEIGHT RANGE TESTS ====================

    @Test
    fun `getIdealWeightRange returns correct range for 170cm`() {
        val (min, max) = BmiCalculator.getIdealWeightRange(170f)
        // 18.5 * 1.7 * 1.7 = 53.47
        // 24.9 * 1.7 * 1.7 = 71.96
        assertEquals(53.47f, min, 0.5f)
        assertEquals(71.96f, max, 0.5f)
    }

    @Test
    fun `getIdealWeightRange returns correct range for 180cm`() {
        val (min, max) = BmiCalculator.getIdealWeightRange(180f)
        // 18.5 * 1.8 * 1.8 = 59.94
        // 24.9 * 1.8 * 1.8 = 80.68
        assertEquals(59.94f, min, 0.5f)
        assertEquals(80.68f, max, 0.5f)
    }

    @Test
    fun `getIdealWeightRange returns zeros for zero height`() {
        val (min, max) = BmiCalculator.getIdealWeightRange(0f)
        assertEquals(0f, min, 0.01f)
        assertEquals(0f, max, 0.01f)
    }

    @Test
    fun `getIdealWeightRange returns zeros for negative height`() {
        val (min, max) = BmiCalculator.getIdealWeightRange(-170f)
        assertEquals(0f, min, 0.01f)
        assertEquals(0f, max, 0.01f)
    }

    // ==================== CATEGORY PROPERTIES TESTS ====================

    @Test
    fun `BmiCategory NORMAL has correct label`() {
        assertEquals("Normal", BmiCalculator.BmiCategory.NORMAL.label)
    }

    @Test
    fun `BmiCategory UNDERWEIGHT has correct label`() {
        assertEquals("Underweight", BmiCalculator.BmiCategory.UNDERWEIGHT.label)
    }

    @Test
    fun `BmiCategory OVERWEIGHT has correct label`() {
        assertEquals("Overweight", BmiCalculator.BmiCategory.OVERWEIGHT.label)
    }

    @Test
    fun `BmiCategory OBESE has correct label`() {
        assertEquals("Obese", BmiCalculator.BmiCategory.OBESE.label)
    }

    @Test
    fun `all categories have non-zero color resources`() {
        BmiCalculator.BmiCategory.values().forEach { category ->
            assertTrue("${category.name} should have valid color", category.colorRes != 0)
        }
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    fun `full flow - calculate and categorize normal weight person`() {
        // 70kg, 175cm person
        val bmi = BmiCalculator.calculate(70f, 175f)
        val category = BmiCalculator.getCategory(bmi)
        val recommendation = BmiCalculator.getRecommendation(category)
        val (minWeight, maxWeight) = BmiCalculator.getIdealWeightRange(175f)

        assertEquals(BmiCalculator.BmiCategory.NORMAL, category)
        assertTrue(70f in minWeight..maxWeight)
        assertTrue(recommendation.isNotEmpty())
    }

    @Test
    fun `full flow - calculate and categorize underweight person`() {
        // 45kg, 170cm person
        val bmi = BmiCalculator.calculate(45f, 170f)
        val category = BmiCalculator.getCategory(bmi)
        val (minWeight, _) = BmiCalculator.getIdealWeightRange(170f)

        assertEquals(BmiCalculator.BmiCategory.UNDERWEIGHT, category)
        assertTrue(45f < minWeight)
    }

    @Test
    fun `full flow - calculate and categorize overweight person`() {
        // 85kg, 170cm person
        val bmi = BmiCalculator.calculate(85f, 170f)
        val category = BmiCalculator.getCategory(bmi)
        val (_, maxWeight) = BmiCalculator.getIdealWeightRange(170f)

        assertEquals(BmiCalculator.BmiCategory.OVERWEIGHT, category)
        assertTrue(85f > maxWeight)
    }
}
