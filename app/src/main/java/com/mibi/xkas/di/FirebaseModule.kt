package com.mibi.xkas.di

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mibi.xkas.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    /**
     * ✅ Provides SharedPreferences untuk dependency injection
     */
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("xkas_preferences", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
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

    @Provides
    @Singleton
    fun provideDebtRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): DebtRepository = DebtRepositoryImpl(firestore, auth)

}