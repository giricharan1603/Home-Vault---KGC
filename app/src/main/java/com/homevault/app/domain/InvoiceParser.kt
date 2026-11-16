package com.homevault.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ParsedInvoice(
    val productName: String,
    val brand: String,
    val modelNumber: String,
    val category: String,
    val priceInr: Double,
    val purchaseDate: LocalDate,
    val warrantyMonths: Int,
    val supportPhone: String
)

object InvoiceParser {
    private const val MAX_OCR_TEXT_LENGTH = 8_000

    private val brandSupportDirectory = mapOf(
        "LG" to ("18003159999" to "Appliance"),
        "Samsung" to ("1800407267864" to "Electronics"),
        "Vivo" to ("18002083388" to "Electronics"),
        "Kent" to ("9278912345" to "Appliance"),
        "Voltas" to ("18605994555" to "Appliance"),
        "Whirlpool" to ("18002081800" to "Appliance"),
        "Sony" to ("18001037799" to "Electronics"),
        "Honda" to ("18001033434" to "Vehicle")
    )

    // Pre-compiled bounded regexes to prevent ReDoS
    private val priceRegex = Regex(
        """(?:₹|Rs\.?|INR|Total[:\s]{1,5})\s*([0-9]{1,3}(?:,[0-9]{2,3}){0,3}(?:\.[0-9]{1,2})?|[0-9]{3,7})""",
        RegexOption.IGNORE_CASE
    )
    private val gstinRegex = Regex("""\b\d{2}[A-Z]{5}\d{4}[A-Z][1-9A-Z]Z[0-9A-Z]\b""")
    private val modelRegex = Regex("""\b([A-Z]{1,4}-?[0-9]{2,5}[A-Z0-9]{0,6})\b""")
    private val dateRegex = Regex("""\b(0[1-9]|[12][0-9]|3[01])[/-](0[1-9]|1[0-2])[/-](20[12][0-9])\b""")
    private val yearWarrantyRegex = Regex("""\b([1-9]|10)\s*Years?\s*Warranty\b""", RegexOption.IGNORE_CASE)
    private val monthWarrantyRegex = Regex("""\b([1-9]|[1-5][0-9]|60)\s*Months?\s*Warranty\b""", RegexOption.IGNORE_CASE)

    private val excludedKeywords = setOf("GSTIN", "INVOICE", "HSN", "CGST", "SGST", "IGST", "TOTAL", "INDIA")

    fun parseRawOcrText(rawInput: String): ParsedInvoice {
        // Bound input size to prevent ReDoS
        val safeText = rawInput.take(MAX_OCR_TEXT_LENGTH).replace(gstinRegex, " ")

        val detectedBrand = brandSupportDirectory.keys.firstOrNull {
            safeText.contains(it, ignoreCase = true)
        } ?: "Verified Brand"

        val (supportNumber, defaultCategory) = brandSupportDirectory[detectedBrand]
            ?: ("1800114000" to "Electronics")

        val matchedPrice = priceRegex.findAll(safeText)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .filter { it in 100.0..2_500_000.0 }
            .maxOrNull() ?: 0.0

        val detectedModel = modelRegex.findAll(safeText)
            .map { it.groupValues[1] }
            .firstOrNull { candidate ->
                excludedKeywords.none { candidate.contains(it, ignoreCase = true) }
            } ?: "MODEL-STD"

        val purchaseDate = dateRegex.find(safeText)?.let { match ->
            runCatching {
                val normalized = match.value.replace("-", "/")
                val parsed = LocalDate.parse(normalized, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                // Reject future dates caused by OCR typos
                if (parsed.isAfter(LocalDate.now())) LocalDate.now() else parsed
            }.getOrNull()
        } ?: LocalDate.now()

        val warrantyMonths = when {
            yearWarrantyRegex.find(safeText) != null -> {
                (yearWarrantyRegex.find(safeText)!!.groupValues[1].toIntOrNull() ?: 1) * 12
            }
            monthWarrantyRegex.find(safeText) != null -> {
                monthWarrantyRegex.find(safeText)!!.groupValues[1].toIntOrNull() ?: 12
            }
            else -> 12
        }.coerceIn(1, 120)

        return ParsedInvoice(
            productName = "$detectedBrand $defaultCategory ($detectedModel)",
            brand = detectedBrand,
            modelNumber = detectedModel,
            category = defaultCategory,
            priceInr = matchedPrice,
            purchaseDate = purchaseDate,
            warrantyMonths = warrantyMonths,
            supportPhone = supportNumber.filter { it.isDigit() || it == '+' }
        )
    }
}