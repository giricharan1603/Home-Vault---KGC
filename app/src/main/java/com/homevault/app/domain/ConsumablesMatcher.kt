package com.homevault.app.domain

import com.homevault.app.data.AssetEntity

data class SparePartRecommendation(
    val partName: String,
    val replacementCycleMonths: Int,
    val estimatedPriceInr: Int,
    val searchKeyword: String
)

object ConsumablesMatcher {

    fun getCompatibleParts(asset: AssetEntity): List<SparePartRecommendation> {
        val brand = asset.brand
        val model = asset.modelNumber
        val nameLower = asset.productName.lowercase()

        return when {
            brand.equals("Kent", ignoreCase = true) || nameLower.contains("ro") || nameLower.contains("purifier") -> listOf(
                SparePartRecommendation(
                    partName = "$brand Sediment & Carbon Filter Kit ($model)",
                    replacementCycleMonths = 6,
                    estimatedPriceInr = 850,
                    searchKeyword = "$brand $model sediment carbon filter replacement kit"
                ),
                SparePartRecommendation(
                    partName = "$brand High-TDS RO Membrane",
                    replacementCycleMonths = 12,
                    estimatedPriceInr = 1650,
                    searchKeyword = "$brand $model original RO membrane cartridge"
                )
            )
            brand.equals("LG", ignoreCase = true) || brand.equals("Whirlpool", ignoreCase = true) || nameLower.contains("refrigerator") -> listOf(
                SparePartRecommendation(
                    partName = "$brand Fresh Air Deodorizer Cartridge",
                    replacementCycleMonths = 6,
                    estimatedPriceInr = 499,
                    searchKeyword = "$brand $model refrigerator air filter deodorizer"
                ),
                SparePartRecommendation(
                    partName = "Heavy-Duty Voltage Stabilizer (4kVA)",
                    replacementCycleMonths = 60,
                    estimatedPriceInr = 2199,
                    searchKeyword = "stabilizer for $brand $model refrigerator"
                )
            )
            brand.equals("Voltas", ignoreCase = true) || nameLower.contains("ac") -> listOf(
                SparePartRecommendation(
                    partName = "$brand Anti-Dust PM2.5 AC Mesh Filter (Pair)",
                    replacementCycleMonths = 6,
                    estimatedPriceInr = 599,
                    searchKeyword = "$brand $model AC dust filter mesh"
                ),
                SparePartRecommendation(
                    partName = "Split AC Foam Jet Cleaning Kit",
                    replacementCycleMonths = 6,
                    estimatedPriceInr = 450,
                    searchKeyword = "split AC coil cleaning foam spray"
                )
            )
            asset.category.equals("Vehicle", ignoreCase = true) || brand.equals("Honda", ignoreCase = true) -> listOf(
                SparePartRecommendation(
                    partName = "$brand Synthetic Engine Oil (10W-30, 1L)",
                    replacementCycleMonths = 6,
                    estimatedPriceInr = 520,
                    searchKeyword = "$brand $model 10W30 synthetic engine oil"
                ),
                SparePartRecommendation(
                    partName = "$brand OEM Viscous Air Filter Element",
                    replacementCycleMonths = 12,
                    estimatedPriceInr = 380,
                    searchKeyword = "$brand $model OEM air filter"
                )
            )
            else -> listOf(
                SparePartRecommendation(
                    partName = "$brand Surge Protector & Care Kit ($model)",
                    replacementCycleMonths = 24,
                    estimatedPriceInr = 699,
                    searchKeyword = "$brand $model original accessories spare parts"
                )
            )
        }
    }
}