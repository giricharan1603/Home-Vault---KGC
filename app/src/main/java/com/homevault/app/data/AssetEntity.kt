package com.homevault.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class AssetStatus { ACTIVE, EXPIRING_SOON, EXPIRED }

@Entity(
    tableName = "assets",
    indices = [Index(value = ["expiryDateEpochDay"])]
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productName: String,
    val brand: String,
    val modelNumber: String,
    val category: String,
    val purchaseDateEpochDay: Long,
    val priceInr: Double,
    val warrantyMonths: Int,
    val expiryDateEpochDay: Long,
    val customerCarePhone: String,
    val sandboxedInvoicePath: String? = null,
    val lastNotifiedDaysRemaining: Long = -1L,
    val cumulativeRepairCostInr: Double = 0.0,
    val lastServiceEpochDay: Long = purchaseDateEpochDay
) {
    fun daysRemaining(todayEpochDay: Long = LocalDate.now().toEpochDay()): Long {
        return expiryDateEpochDay - todayEpochDay
    }

    fun status(todayEpochDay: Long = LocalDate.now().toEpochDay()): AssetStatus {
        val remaining = daysRemaining(todayEpochDay)
        return when {
            remaining < 0 -> AssetStatus.EXPIRED
            remaining <= 30 -> AssetStatus.EXPIRING_SOON
            else -> AssetStatus.ACTIVE
        }
    }
}