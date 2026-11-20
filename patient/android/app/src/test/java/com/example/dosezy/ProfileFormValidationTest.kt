/*
 * Copyright (c) 2026 Saad <reach.saad@outlook.com> (@saad2134)
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

package com.example.dosezy

import com.example.dosezy.data.model.Gender
import com.example.dosezy.ui.screens.newuser.validateProfileForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileFormValidationTest {

    @Test
    fun testValidateProfileForm_validInputs_returnsValid() {
        val result = validateProfileForm(
            fullName = "John Doe",
            ageText = "35",
            gender = Gender.MALE
        )
        assertTrue("Complete profile form must be valid", result.isValid)
        assertTrue("Name must be valid", result.isNameValid)
        assertTrue("Age must be valid", result.isAgeValid)
        assertTrue("Gender must be valid", result.isGenderValid)
    }

    @Test
    fun testValidateProfileForm_blankName_returnsInvalidName() {
        val result = validateProfileForm(
            fullName = "   ",
            ageText = "25",
            gender = Gender.FEMALE
        )
        assertFalse("Blank name must invalidate form", result.isValid)
        assertFalse("Name should be marked invalid", result.isNameValid)
        assertTrue("Age should remain valid", result.isAgeValid)
        assertTrue("Gender should remain valid", result.isGenderValid)
    }

    @Test
    fun testValidateProfileForm_invalidAge_returnsInvalidAge() {
        val zeroAgeResult = validateProfileForm(
            fullName = "Jane Doe",
            ageText = "0",
            gender = Gender.FEMALE
        )
        assertFalse("Age of 0 must be invalid", zeroAgeResult.isValid)
        assertFalse("Age should be marked invalid", zeroAgeResult.isAgeValid)

        val blankAgeResult = validateProfileForm(
            fullName = "Jane Doe",
            ageText = "",
            gender = Gender.FEMALE
        )
        assertFalse("Blank age must be invalid", blankAgeResult.isValid)
        assertFalse("Age should be marked invalid", blankAgeResult.isAgeValid)
    }

    @Test
    fun testValidateProfileForm_arabicDigitsInAge_parsesAccurately() {
        // Arabic-Indic numeral for 28: ٢٨
        val result = validateProfileForm(
            fullName = "Ahmed",
            ageText = "٢٨",
            gender = Gender.MALE
        )
        assertTrue("Arabic-Indic digits must parse cleanly and mark form valid", result.isValid)
        assertTrue("Age must be valid", result.isAgeValid)
    }

    @Test
    fun testValidateProfileForm_nullGender_returnsInvalidGender() {
        val result = validateProfileForm(
            fullName = "Alex",
            ageText = "40",
            gender = null
        )
        assertFalse("Null gender must invalidate form", result.isValid)
        assertFalse("Gender should be marked invalid", result.isGenderValid)
        assertTrue("Name should remain valid", result.isNameValid)
        assertTrue("Age should remain valid", result.isAgeValid)
    }

    @Test
    fun testValidateProfileForm_allFieldsEmpty_returnsAllInvalid() {
        val result = validateProfileForm(
            fullName = "",
            ageText = "",
            gender = null
        )
        assertFalse("Empty profile form must be invalid", result.isValid)
        assertFalse("Name should be invalid", result.isNameValid)
        assertFalse("Age should be invalid", result.isAgeValid)
        assertFalse("Gender should be invalid", result.isGenderValid)
    }
}

