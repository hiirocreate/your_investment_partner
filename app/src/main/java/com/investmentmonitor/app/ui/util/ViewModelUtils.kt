package com.investmentmonitor.app.ui.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Small helper so each screen can build its ViewModel from [com.investmentmonitor.app.ServiceLocator]
 * without pulling in a DI framework (see ServiceLocator's doc comment for the rationale).
 */
inline fun <reified VM : ViewModel> simpleViewModelFactory(crossinline create: () -> VM): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { create() }
    }
