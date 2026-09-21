package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SmartSalesRepository
import com.example.model.User
import com.example.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: SmartSalesRepository) : ViewModel() {

    val currentUser: StateFlow<User?> = repository.currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Please enter both email and password."
            return
        }
        _isLoading.value = true
        _errorMessage.value = null
        repository.login(email.trim(), pass.trim()) { result ->
            _isLoading.value = false
            result.onSuccess { user ->
                _successMessage.value = "Welcome back, ${user.name}!"
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Authentication failed. Please check your credentials."
            }
        }
    }

    fun register(name: String, email: String, pass: String, role: UserRole) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "All fields are required."
            return
        }
        if (pass.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters long."
            return
        }
        _isLoading.value = true
        _errorMessage.value = null
        repository.register(name.trim(), email.trim(), pass.trim(), role) { result ->
            _isLoading.value = false
            result.onSuccess { user ->
                _successMessage.value = "Account created successfully as ${role.displayName}!"
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Registration failed. Please try again."
            }
        }
    }

    fun quickDemoLogin(role: UserRole) {
        _errorMessage.value = null
        val email = if (role == UserRole.ADMIN) "admin@smartsales.com" else "staff@smartsales.com"
        login(email, "password123")
    }

    fun switchRole(role: UserRole) {
        repository.switchDemoRole(role)
        _successMessage.value = "Switched active view to ${role.displayName}"
    }

    fun logout() {
        repository.logout()
        _successMessage.value = "Logged out successfully."
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
