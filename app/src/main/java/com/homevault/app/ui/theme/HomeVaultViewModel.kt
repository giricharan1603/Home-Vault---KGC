package com.homevault.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.homevault.app.data.AssetEntity
import com.homevault.app.data.HomeVaultDatabase
import com.homevault.app.domain.InvoiceParser
import com.homevault.app.scanner.ReceiptOcrHelper
import com.homevault.app.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class HomeVaultViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = HomeVaultDatabase.getInstance(application).assetDao()

    val assets: StateFlow<List<AssetEntity>> = dao.getAllAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _scanFeedback = MutableStateFlow<String?>(null)
    val scanFeedback: StateFlow<String?> = _scanFeedback.asStateFlow()

    fun addAssetFromOcrText(rawText: String, sandboxedPath: String? = null) {
        if (rawText.isBlank()) {
            _scanFeedback.value = "No readable text found on invoice."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val parsed = InvoiceParser.parseRawOcrText(rawText)
            val expiryDate = parsed.purchaseDate.plusMonths(parsed.warrantyMonths.toLong())

            val entity = AssetEntity(
                productName = parsed.productName,
                brand = parsed.brand,
                modelNumber = parsed.modelNumber,
                category = parsed.category,
                purchaseDateEpochDay = parsed.purchaseDate.toEpochDay(),
                priceInr = parsed.priceInr,
                warrantyMonths = parsed.warrantyMonths,
                expiryDateEpochDay = expiryDate.toEpochDay(),
                customerCarePhone = parsed.supportPhone,
                sandboxedInvoicePath = sandboxedPath
            )
            dao.insertAsset(entity)
            _scanFeedback.value = "Encrypted & Saved: ${parsed.productName}"
        }
    }

    fun processSharedUri(externalUri: Uri) {
        viewModelScope.launch {
            runCatching {
                val app = getApplication<Application>()
                val sandboxedFile = SecurityManager.copySharedInvoiceToSandbox(app, externalUri)
                val ocrText = ReceiptOcrHelper.recognizeFromSandboxedFile(app, sandboxedFile)
                addAssetFromOcrText(ocrText, sandboxedFile.absolutePath)
            }.onFailure { e ->
                _scanFeedback.value = "Security check failed: ${e.localizedMessage}"
            }
        }
    }

    fun deleteAsset(asset: AssetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            asset.sandboxedInvoicePath?.let { path ->
                runCatching { File(path).delete() }
            }
            dao.deleteAsset(asset)
        }
    }

    fun clearFeedback() {
        _scanFeedback.value = null
    }
}