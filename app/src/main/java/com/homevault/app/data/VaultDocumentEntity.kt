package com.homevault.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "vault_documents")
data class VaultDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val docTitle: String,
    val docType: String,
    val registrationOrPolicyNo: String,
    val expiryDateEpochDay: Long
) {
    fun daysRemaining(todayEpochDay: Long = LocalDate.now().toEpochDay()): Long {
        return expiryDateEpochDay - todayEpochDay
    }
}