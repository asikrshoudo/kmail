package com.kmail.thekami.ui.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupViewModelTest {
    private val vm = SetupViewModel()

    @Test
    fun invalidEmailIsRejectedAndFlagged() {
        vm.onEmailChange("not-an-email")
        vm.onPasswordChange("secret")
        assertFalse(vm.submitCredentials())
        assertTrue(vm.credentials.value.emailError)
        assertFalse(vm.credentials.value.passwordError)
    }

    @Test
    fun emptyPasswordIsRejectedAndFlagged() {
        vm.onEmailChange("me@example.com")
        assertFalse(vm.submitCredentials())
        assertFalse(vm.credentials.value.emailError)
        assertTrue(vm.credentials.value.passwordError)
    }

    @Test
    fun bothErrorsAreReportedTogether() {
        assertFalse(vm.submitCredentials())
        assertTrue(vm.credentials.value.emailError)
        assertTrue(vm.credentials.value.passwordError)
    }

    @Test
    fun validInputIsAcceptedEvenWithPaddingAndCase() {
        vm.onEmailChange("  Me@Example.com ")
        vm.onPasswordChange(" spaces are allowed ")
        assertTrue(vm.submitCredentials())
        assertFalse(vm.credentials.value.emailError)
        assertFalse(vm.credentials.value.passwordError)
    }

    @Test
    fun editingAFieldClearsItsError() {
        vm.submitCredentials()
        vm.onEmailChange("a")
        assertFalse(vm.credentials.value.emailError)
        assertTrue(vm.credentials.value.passwordError)
        vm.onPasswordChange("x")
        assertFalse(vm.credentials.value.passwordError)
    }
}
