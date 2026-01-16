package com.kia.blagogramapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.LoginResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class VerificationState {
    object Idle : VerificationState()
    object Loading : VerificationState() // Состояние проверки при нажатии кнопки
    data class Success(val result: LoginResult) : VerificationState()
    data class Error(val message: String) : VerificationState()
}

class RegisterViewModel : ViewModel() {

    private val _state = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val state: StateFlow<VerificationState> = _state.asStateFlow()

    /**
     * Ручная проверка статуса регистрации по кнопке "Подтвердил"
     */
    fun checkStatus(verifyToken: String) {
        viewModelScope.launch {
            _state.value = VerificationState.Loading
            
            try {
                // Вызываем API (auth.verify.check)
                val response = ApiClient.api.checkVerification(token = verifyToken)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.ok && body.result?.token?.isNotBlank() == true) {
                        Log.d("RegisterVM", "Success! User verified.")
                        _state.value = VerificationState.Success(body.result)
                    } else {
                        Log.d("RegisterVM", "Verified: false or no token")
                        _state.value = VerificationState.Error("Вы еще не подтвердили номер в боте. Сделайте это и нажмите кнопку снова.")
                    }
                } else {
                    val code = response.code()
                    if (code == 400) {
                        _state.value = VerificationState.Error("Подтверждение еще не получено. Попробуйте через мгновение.")
                    } else {
                        _state.value = VerificationState.Error("Ошибка сервера ($code). Попробуйте позже.")
                    }
                }
            } catch (e: Exception) {
                Log.e("RegisterVM", "Check error", e)
                _state.value = VerificationState.Error("Ошибка сети. Проверьте интернет.")
            }
        }
    }

    /**
     * Сброс состояния в Idle, чтобы убрать ошибки и показать кнопку снова
     */
    fun resetState() {
        _state.value = VerificationState.Idle
    }
}
