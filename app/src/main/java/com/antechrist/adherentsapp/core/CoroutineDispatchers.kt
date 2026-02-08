// com/antechrist/adherentsapp/core/CoroutineDispatchers.kt
package com.antechrist.adherentsapp.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Pack de Dispatchers injectables (IO/Default/Main/Unconfined).
 * Fourni par votre DispatcherModule Hilt existant.
 */
data class CoroutineDispatchers(
    val io: CoroutineDispatcher = Dispatchers.IO,
    val default: CoroutineDispatcher = Dispatchers.Default,
    val main: CoroutineDispatcher = Dispatchers.Main,
    val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
)
