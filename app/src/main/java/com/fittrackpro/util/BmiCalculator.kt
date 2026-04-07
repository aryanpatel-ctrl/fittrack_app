package com.fittrackpro.util

import com.fittrackpro.R

/**
 * BMI Calculator utility for calculating Body Mass Index
 * and determining health categories
 */
object BmiCalculator {

    /**
     * Calculate BMI from weight and height
     * @param weightKg Weight in kilograms
     * @param heightCm Height in centimeters
     * @return BMI value
     */
    fun calculate(weightKg: Float, heightCm: Float): Float {
        if (weightKg <= 0 || heightCm <= 0) return 0f
        val heightM = heightCm / 100f
        return weightKg / (heightM * heightM)
    }

    /**
     * Get BMI category based on BMI value
     * Uses WHO classification
     */
    fun getCategory(bmi: Float): BmiCategory {
        return when {
            bmi <= 0 -> BmiCategory.UNKNOWN
            bmi < 18.5f -> BmiCategory.UNDERWEIGHT
            bmi < 25.0f -> BmiCategory.NORMAL
            bmi < 30.0f -> BmiCategory.OVERWEIGHT
            else -> BmiCategory.OBESE
        }
    }

    /**
     * Get health recommendation based on BMI category
     */
    fun getRecommendation(category: BmiCategory): String {
        return when (category) {
            BmiCategory.UNDERWEIGHT -> "Consider increasing calorie intake with nutritious foods"
            BmiCategory.NORMAL -> "Great job! Maintain your healthy lifestyle"
            BmiCategory.OVERWEIGHT -> "Regular exercise and balanced diet recommended"
            BmiCategory.OBESE -> "Consult a healthcare provider for personalized advice"
            BmiCategory.UNKNOWN -> "Enter your height and weight to calculate BMI"
        }
    }

    /**
     * Get ideal weight range for a given height
     * Based on BMI range of 18.5-24.9
     */
    fun getIdealWeightRange(heightCm: Float): Pair<Float, Float> {
        if (heightCm <= 0) return Pair(0f, 0f)
        val heightM = heightCm / 100f
        val minWeight = 18.5f * heightM * heightM
        val maxWeight = 24.9f * heightM * heightM
        return Pair(minWeight, maxWeight)
    }

    enum class BmiCategory(
        val label: String,
        val colorRes: Int,
        val emoji: String
    ) {
        UNDERWEIGHT("Underweight", R.color.warning, ""),
        NORMAL("Normal", R.color.success, ""),
        OVERWEIGHT("Overweight", R.color.warning, ""),
        OBESE("Obese", R.color.error, ""),
        UNKNOWN("Unknown", R.color.text_secondary, "")
    }
}
