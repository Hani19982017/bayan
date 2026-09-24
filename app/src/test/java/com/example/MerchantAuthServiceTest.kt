package com.example

import com.example.util.MerchantAuthService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantAuthServiceTest {

    @Test
    fun testEmailNormalization() {
        assertEquals("merchant@gmail.com", MerchantAuthService.normalizeEmail("Merchant@Gmail.com"))
        assertEquals("merchant@gmail.com", MerchantAuthService.normalizeEmail("  MERCHANT@GMAIL.COM  "))
        assertEquals("user123@domain.org", MerchantAuthService.normalizeEmail(" User123@Domain.ORG "))
        assertEquals("menesy", MerchantAuthService.normalizeEmail("  MeNeSy  "))
    }

    @Test
    fun testEmailValidation() {
        assertTrue(MerchantAuthService.isValidEmail("merchant@gmail.com"))
        assertTrue(MerchantAuthService.isValidEmail("test.user+tag@sub.domain.co"))
        assertFalse(MerchantAuthService.isValidEmail("plainaddress"))
        assertFalse(MerchantAuthService.isValidEmail("@missingusername.com"))
        assertFalse(MerchantAuthService.isValidEmail("missingdomain@"))
        assertFalse(MerchantAuthService.isValidEmail(""))
    }

    @Test
    fun testDuplicateEmailPolicySameEmailDifferentStoreName() {
        val existingRegistry = mutableMapOf<String, String>()
        val existingEmail = MerchantAuthService.normalizeEmail("merchant@gmail.com")
        existingRegistry[existingEmail] = "متجر أحمد"

        // Scenario 1: Same Email + Same Store Name -> MUST BE REJECTED
        val attempt1Email = MerchantAuthService.normalizeEmail("merchant@gmail.com")
        val attempt1Store = "متجر أحمد"
        val isDuplicate1 = existingRegistry.containsKey(attempt1Email)
        assertTrue("نفس Email + نفس Store Name يجب أن يتم رفضه", isDuplicate1)

        // Scenario 2: Same Email + Different Store Name -> MUST BE REJECTED
        val attempt2Email = MerchantAuthService.normalizeEmail("merchant@gmail.com")
        val attempt2Store = "متجر محمد"
        val isDuplicate2 = existingRegistry.containsKey(attempt2Email)
        assertTrue("نفس Email + Store Name مختلف يجب أن يتم رفضه", isDuplicate2)

        // Scenario 3: Different Email + Same Store Name -> MUST BE ALLOWED
        val attempt3Email = MerchantAuthService.normalizeEmail("another.merchant@gmail.com")
        val attempt3Store = "متجر أحمد"
        val isDuplicate3 = existingRegistry.containsKey(attempt3Email)
        assertFalse("Email مختلف + نفس Store Name مسموح", isDuplicate3)

        // Scenario 4: Uppercase vs Lowercase of same Email -> MUST BE TREATED AS IDENTICAL & REJECTED
        val attempt4Email = MerchantAuthService.normalizeEmail("  MeRcHaNt@GMAIL.COM ")
        val isDuplicate4 = existingRegistry.containsKey(attempt4Email)
        assertTrue("Email بحروف Uppercase/Lowercase مختلفة يجب اعتباره نفس البريد ورفضه", isDuplicate4)
    }

    @Test
    fun testErrorMessageMatchesRequirement() {
        val expectedMessage = "هذا البريد الإلكتروني مستخدم بالفعل. لا يمكن إنشاء أكثر من حساب تاجر بنفس البريد الإلكتروني."
        assertEquals(expectedMessage, MerchantAuthService.ERR_DUPLICATE_EMAIL)
    }
}
