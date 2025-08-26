package com.mibi.xkas.ui.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mibi.xkas.data.model.DebtContact
import com.mibi.xkas.data.repository.DebtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ContactDebtViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val contacts: StateFlow<List<DebtContact>> = flow {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            debtRepository.getAllDebtContacts(userId).collect { emit(it) }
        } else {
            emit(emptyList())
        }
    }.catch { e ->
        _error.value = "Gagal memuat data: ${e.message}"
        emit(emptyList())
    }.onStart {
        _loading.value = true
    }.onEach {
        _loading.value = false
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun refreshContacts() {
        _loading.value = true
        _error.value = null
    }
}
