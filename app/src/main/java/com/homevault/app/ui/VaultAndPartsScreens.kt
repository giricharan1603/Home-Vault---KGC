package com.homevault.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.homevault.app.data.AssetEntity
import com.homevault.app.data.VaultDocumentEntity
import com.homevault.app.domain.ConsumablesMatcher
import com.homevault.app.security.BiometricVaultAuthenticator
import java.time.LocalDate

@Composable
fun BiometricComplianceVaultScreen(
    documents: List<VaultDocumentEntity>,
    onAddSampleDoc: () -> Unit,
    onDeleteDoc: (VaultDocumentEntity) -> Unit,
    onAuthError: (String) -> Unit
) {
    val context = LocalContext.current
    var isUnlocked by remember { mutableStateOf(false) }

    if (!isUnlocked) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Hardware Keystore Vault Locked",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Your Vehicle RC, PUC, Motor Insurance, and ID records are encrypted with AES-256.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            BiometricVaultAuthenticator.authenticate(
                                activity = activity,
                                onSuccess = { isUnlocked = true },
                                onError = onAuthError
                            )
                        } else {
                            onAuthError("Unable to access Biometric hardware context.")
                        }
                    }
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Authenticate to Unlock")
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Compliance & Vehicle Vault", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row {
                TextButton(onClick = onAddSampleDoc) {
                    Text("+ Sample PUC/RC")
                }
                IconButton(onClick = { isUnlocked = false }) {
                    Icon(Icons.Default.LockOpen, contentDescription = "Lock Vault")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (documents.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No compliance documents yet. Tap '+ Sample PUC/RC' to test.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(documents, key = { it.id }) { doc ->
                    val days = doc.daysRemaining()
                    val statusColor = if (days <= 15) Color(0xFFC62828) else Color(0xFF2E7D32)

                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.docTitle, fontWeight = FontWeight.Bold)
                                Text("Type: ${doc.docType} • Reg/Policy: ${doc.registrationOrPolicyNo}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "Valid Till: ${LocalDate.ofEpochDay(doc.expiryDateEpochDay)} ($days days left)",
                                    color = statusColor,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            IconButton(onClick = { onDeleteDoc(doc) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SparePartsBottomSheet(
    asset: AssetEntity,
    onDismiss: () -> Unit,
    onLaunchError: (String) -> Unit
) {
    val context = LocalContext.current
    val parts = remember(asset) { ConsumablesMatcher.getCompatibleParts(asset) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Compatible Parts & Consumables",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Matched for ${asset.brand} (${asset.modelNumber})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            parts.forEach { part ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(part.partName, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Replace every ${part.replacementCycleMonths} months • Est. ₹${part.estimatedPriceInr}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                runCatching {
                                    val uri = Uri.Builder()
                                        .scheme("https")
                                        .authority("www.amazon.in")
                                        .path("s")
                                        .appendQueryParameter("k", part.searchKeyword)
                                        .build()
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                }.onFailure {
                                    onLaunchError("Could not open browser.")
                                }
                            }
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Buy")
                        }
                    }
                }
            }
        }
    }
}