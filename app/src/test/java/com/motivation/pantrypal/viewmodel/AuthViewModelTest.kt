package com.motivation.pantrypal.viewmodel

import com.motivation.pantrypal.data.remote.dto.UserDto
import com.motivation.pantrypal.data.repository.AuthRepository
import com.motivation.pantrypal.ui.auth.AuthViewModel
import com.motivation.pantrypal.util.MainDispatcherRule
import com.motivation.pantrypal.util.PantryResult
import com.motivation.pantrypal.util.mockAndroidLog
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        mockAndroidLog()
        viewModel = AuthViewModel(authRepository)
    }

    @Test
    fun `register rejects mismatched passwords without calling the repository`() = runTest {
        viewModel.register("Sam H", "sam@example.com", "password1", "password2")
        advanceUntilIdle()

        assertEquals("Passwords do not match", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun `register rejects a password shorter than 8 characters`() = runTest {
        viewModel.register("Sam H", "sam@example.com", "short", "short")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage!!.contains("8+ characters"))
    }

    @Test
    fun `register succeeds and marks the user authenticated on repository success`() = runTest {
        val user = UserDto(id = "u1", fullName = "Sam H", email = "sam@example.com")
        coEvery { authRepository.register(any(), any(), any()) } returns PantryResult.Success(user)

        viewModel.register("Sam H", "sam@example.com", "password1", "password1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `login surfaces the repository error message on failure`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns PantryResult.Error("Invalid credentials")

        viewModel.login("sam@example.com", "wrongpassword")
        advanceUntilIdle()

        assertEquals("Invalid credentials", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun `login rejects blank fields locally`() = runTest {
        viewModel.login("", "")
        advanceUntilIdle()

        assertEquals("Enter your email and password", viewModel.uiState.value.errorMessage)
    }
}
