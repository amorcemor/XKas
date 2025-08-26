package com.mibi.xkas.ui.home

import com.mibi.xkas.data.Transaction

sealed class TransactionListUiState {
    object Loading : TransactionListUiState()
    data class Success(val transactions: List<Transaction>) : TransactionListUiState()
    data class Error(val message: String) : TransactionListUiState()
    object Empty : TransactionListUiState()
    object FirstTimeEmpty : TransactionListUiState()
    object NoResultsForFilter : TransactionListUiState()
    object SelectBusinessUnit : TransactionListUiState()
}
