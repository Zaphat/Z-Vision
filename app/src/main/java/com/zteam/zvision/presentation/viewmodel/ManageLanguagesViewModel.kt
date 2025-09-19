package com.zteam.zvision.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zteam.zvision.domain.usecase.TranslatorUsecase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageLanguagesViewModel @Inject constructor(
    private val usecase: TranslatorUsecase
) : ViewModel() {
    private val _offline = MutableStateFlow<Set<String>>(emptySet())
    val offline: StateFlow<Set<String>> = _offline.asStateFlow()

    private val _all = MutableStateFlow<Set<String>>(emptySet())
    val all: StateFlow<Set<String>> = _all.asStateFlow()

    init {
        viewModelScope.launch {
            _all.value = usecase.getAllSupportedLanguages()
            _offline.value = usecase.getOfflineLanguages()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _offline.value = usecase.getOfflineLanguages()
        }
    }

    fun download(iso: String) {
        viewModelScope.launch {
            usecase.downloadModel(iso, requireWifi = true)
            _offline.value = usecase.getOfflineLanguages()
        }
    }

    fun delete(iso: String) {
        viewModelScope.launch {
            usecase.deleteModel(iso)
            _offline.value = usecase.getOfflineLanguages()
        }
    }
}
