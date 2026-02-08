package com.antechrist.adherentsapp.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antechrist.adherentsapp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogoutViewModel @Inject constructor(
    private val auth: AuthRepository
) : ViewModel() {
    fun logout(onDone: () -> Unit) = viewModelScope.launch {
        auth.signOut()
        onDone()
    }
}
