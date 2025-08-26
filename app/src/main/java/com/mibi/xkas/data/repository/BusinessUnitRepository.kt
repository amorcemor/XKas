package com.mibi.xkas.data.repository

import com.mibi.xkas.data.BusinessUnit
import kotlinx.coroutines.flow.Flow

interface BusinessUnitRepository {

    suspend fun createBusinessUnit(businessUnit: BusinessUnit): Result<String> // userId akan diambil dari objek BusinessUnit

    fun getUserBusinessUnit(userId: String): Flow<List<BusinessUnit>>

    fun getBusinessUnit(businessUnitId: String): Flow<BusinessUnit?>

    suspend fun updateBusinessUnit(businessUnit: BusinessUnit): Result<Unit>

    suspend fun deleteBusinessUnit(businessUnitId: String): Result<Unit>
}
