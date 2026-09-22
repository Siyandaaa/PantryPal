package com.motivation.pantrypal.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Tiny generic factory so every ViewModel can take [AppContainer] dependencies in its constructor. */
class ViewModelFactory(private val create: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}
