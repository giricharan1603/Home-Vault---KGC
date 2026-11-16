package com.homevault.app.data

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY expiryDateEpochDay ASC")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets")
    suspend fun getAllAssetsOnce(): List<AssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity): Long

    @Query("UPDATE assets SET lastNotifiedDaysRemaining = :daysRemaining WHERE id = :assetId")
    suspend fun markNotificationSent(assetId: Long, daysRemaining: Long)

    @Delete
    suspend fun deleteAsset(asset: AssetEntity)

    // Biometric Vault Queries
    @Query("SELECT * FROM vault_documents ORDER BY expiryDateEpochDay ASC")
    fun getAllVaultDocuments(): Flow<List<VaultDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultDocument(doc: VaultDocumentEntity)

    @Delete
    suspend fun deleteVaultDocument(doc: VaultDocumentEntity)
}