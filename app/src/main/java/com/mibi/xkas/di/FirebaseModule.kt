package com.mibi.xkas.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mibi.xkas.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()


    // Existing Repositories
    fun provideTransactionRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): TransactionRepository = TransactionRepository(firestore, auth)


    @Provides
    @Singleton
    fun provideBusinessUnitRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): BusinessUnitRepository = BusinessUnitRepositoryImpl(firestore, auth)

    // ✅ New: Debt Repository
    @Provides
    @Singleton
    fun provideDebtRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): DebtRepository = DebtRepositoryImpl(firestore, auth)
}
