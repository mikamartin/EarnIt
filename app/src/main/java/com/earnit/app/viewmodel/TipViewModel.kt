package com.earnit.app.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earnit.app.data.PurchaseResult
import com.earnit.app.data.TipOption
import com.earnit.app.data.TipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TipViewModel
    @Inject
    constructor(
        private val repository: TipRepository,
    ) : ViewModel() {
        sealed class TipState {
            object Loading : TipState()

            data class Ready(
                val options: List<TipOption>,
            ) : TipState()

            object Error : TipState()
        }

        sealed class PurchaseEvent {
            object Success : PurchaseEvent()

            object Cancelled : PurchaseEvent()

            data class Error(
                val message: String,
            ) : PurchaseEvent()
        }

        private val _tipState = MutableStateFlow<TipState>(TipState.Loading)
        val tipState: StateFlow<TipState> = _tipState.asStateFlow()

        private val _purchaseEvent = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 1)
        val purchaseEvent: SharedFlow<PurchaseEvent> = _purchaseEvent.asSharedFlow()

        init {
            loadOptions()
        }

        private fun loadOptions() {
            viewModelScope.launch {
                _tipState.value = TipState.Loading
                _tipState.value =
                    try {
                        TipState.Ready(repository.fetchTipOptions())
                    } catch (e: Exception) {
                        TipState.Error
                    }
            }
        }

        fun purchase(
            activity: Activity,
            productId: String,
        ) {
            viewModelScope.launch {
                val event =
                    when (val result = repository.purchase(activity, productId)) {
                        is PurchaseResult.Success -> PurchaseEvent.Success
                        is PurchaseResult.Cancelled -> PurchaseEvent.Cancelled
                        is PurchaseResult.Error -> PurchaseEvent.Error(result.message)
                    }
                _purchaseEvent.emit(event)
            }
        }
    }
