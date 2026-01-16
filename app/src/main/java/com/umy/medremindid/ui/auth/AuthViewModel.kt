package com.umy.medremindid.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umy.medremindid.data.repository.AuthRepository
import com.umy.medremindid.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val message: String? = null
)

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun register(fullName: String, email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            when (val res = repo.register(fullName, email, password)) {
                is AuthResult.Success -> {
                    _state.value = AuthUiState(loading = false)
                    onSuccess()
                }
                is AuthResult.Error -> _state.value = AuthUiState(loading = false, message = res.message)
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            when (val res = repo.login(email, password)) {
                is AuthResult.Success -> {
                    _state.value = AuthUiState(loading = false)
                    onSuccess()
                }
                is AuthResult.Error -> _state.value = AuthUiState(loading = false, message = res.message)
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.logout()
            onDone()
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
