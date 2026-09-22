package com.motivation.pantrypal.viewmodel

import com.motivation.pantrypal.data.prefs.SessionManager
import com.motivation.pantrypal.data.remote.dto.UserDto
import com.motivation.pantrypal.data.repository.AuthRepository
import com.motivation.pantrypal.ui.profile.ProfileViewModel
import com.motivation.pantrypal.util.MainDispatcherRule
import com.motivation.pantrypal.util.PantryResult
import com.motivation.pantrypal.util.mockAndroidLog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        mockAndroidLog()
        every { sessionManager.userName } returns flowOf("Sam Hlatshwayo")
        every { sessionManager.darkThemeEnabled } returns flowOf(false)
        coEvery { authRepository.syncNow() } returns PantryResult.Success(
            UserDto(id = "u1", fullName = "Sam Hlatshwayo", email = "sam@example.com")
        )
        viewModel = ProfileViewModel(authRepository, sessionManager)
    }

    @Test
    fun `toggling a dietary preference adds it then removes it on second tap`() = runTest {
        advanceUntilIdle()

        viewModel.toggleDietaryPreference("Vegetarian")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.dietaryPreferences.contains("Vegetarian"))

        viewModel.toggleDietaryPreference("Vegetarian")
        advanceUntilIdle()
        assertTrue(!viewModel.uiState.value.dietaryPreferences.contains("Vegetarian"))
    }

    @Test
    fun `toggling a preference persists it to the server via updateProfile`() = runTest {
        advanceUntilIdle()

        viewModel.toggleAllergy("Peanuts")
        advanceUntilIdle()

        coVerify { authRepository.updateProfile(dietaryPreferences = any(), allergies = match { it.contains("Peanuts") }) }
    }

    @Test
    fun `setDarkTheme persists to both DataStore and the server`() = runTest {
        advanceUntilIdle()

        viewModel.setDarkTheme(true)
        advanceUntilIdle()

        coVerify { sessionManager.setDarkTheme(true) }
        coVerify { authRepository.updateProfile(theme = "dark") }
    }

    @Test
    fun `logout clears the session and invokes the completion callback`() = runTest {
        var loggedOut = false

        viewModel.logout { loggedOut = true }
        advanceUntilIdle()

        coVerify { authRepository.logout() }
        assertTrue(loggedOut)
    }
}
