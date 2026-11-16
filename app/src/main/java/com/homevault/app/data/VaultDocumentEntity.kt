package com.homevault.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "vault_documents")
data class VaultDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val docTitle: String,          // e.g., "Honda Activa 6G - PUC Certificate"
    val docType: String,           // "RC", "PUC", "Insurance", "Driving Licence"
    val registrationOrPolicyNo: String,
    val expiryDateEpochDay: Long
) {
    fun daysRemaining(todayEpochDay: Long = LocalDate.now().toEpochDay()): Long {
        return expiryDateEpochDay - todayEpochDay
    }
}