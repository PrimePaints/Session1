package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.UserTier
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.RecRed

/**
 * Free vs PRO dialog. Purchases go through Google Play Billing:
 * [onBuyPro] launches the billing flow, [onRestore] re-queries owned purchases.
 * [proPrice] is Play's localised price (null until the store responds).
 * [onDebugTogglePro] is non-null only on debug builds — local entitlement toggle
 * for development, since billing needs a Play-distributed build.
 */
@Composable
fun ProUpsellDialog(
    currentTier: UserTier = UserTier.FREE,
    proPrice: String? = null,
    onDismiss: () -> Unit,
    onBuyPro: () -> Unit,
    onRestore: () -> Unit,
    onDebugTogglePro: ((Boolean) -> Unit)? = null
) {
    var selectedTierTab by remember { mutableStateOf(if (currentTier == UserTier.FREE) UserTier.PRO else currentTier) }
    val isPro = currentTier == UserTier.PRO

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Repeatless PRO",
                        tint = AmberAccent,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Repeatless PRO", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        },
        text = {
            Column {
                Text(
                    text = if (isPro) "PRO is active on this device. Thank you!"
                    else "Unlock everything with a single one-time purchase — no subscription.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tier Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    TierTabButton(
                        title = "Standard",
                        badge = "FREE",
                        isSelected = selectedTierTab == UserTier.FREE,
                        onClick = { selectedTierTab = UserTier.FREE },
                        modifier = Modifier.weight(1f)
                    )

                    TierTabButton(
                        title = "PRO",
                        badge = proPrice ?: "One-time",
                        isSelected = selectedTierTab == UserTier.PRO,
                        onClick = { selectedTierTab = UserTier.PRO },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTierTab) {
                    UserTier.FREE -> {
                        TierDetailCard(
                            tierName = "Standard (Free)",
                            description = "Essential soundboard features for everyday quick taps.",
                            features = listOf(
                                "Up to 2 Soundboards" to true,
                                "Custom Audio Recording" to true,
                                "1 Home-Screen Widget" to true,
                                "Auto-Parent AI Listening" to false,
                                "Unlimited Boards & Trimming" to false
                            ),
                            accentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    UserTier.PRO -> {
                        TierDetailCard(
                            tierName = "Repeatless PRO",
                            description = "Everything unlocked, forever. Pay once, nag effortlessly.",
                            features = listOf(
                                "Unlimited Boards & Sound Clips" to true,
                                "⚡ Auto-Parent AI Listening Mode" to true,
                                "Audio Waveform Trimming & Edits" to true,
                                "Custom Acoustic Trigger Tags" to true,
                                "All Themes & Colour Packs" to true,
                                "One-time purchase — no subscription" to true
                            ),
                            accentColor = AmberAccent
                        )
                    }
                }

                if (onDebugTogglePro != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "(Debug) Simulate PRO entitlement",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = isPro, onCheckedChange = { onDebugTogglePro(it) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isPro && selectedTierTab == UserTier.PRO) {
                        onBuyPro()
                    }
                    onDismiss()
                },
                enabled = !isPro && selectedTierTab == UserTier.PRO,
                colors = ButtonDefaults.buttonColors(containerColor = RecRed)
            ) {
                Text(
                    text = when {
                        isPro -> "PRO Active ✓"
                        selectedTierTab == UserTier.PRO ->
                            if (proPrice != null) "Unlock PRO · $proPrice" else "Unlock PRO"
                        else -> "Free plan"
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRestore) {
                    Text("Restore purchases", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}

@Composable
private fun TierTabButton(
    title: String,
    badge: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AmberAccent else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = badge,
                fontSize = 10.sp,
                color = if (isSelected) Color.Black.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TierDetailCard(
    tierName: String,
    description: String,
    features: List<Pair<String, Boolean>>,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = tierName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = accentColor)
            Text(text = description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(10.dp))

            features.forEach { (feature, isIncluded) ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isIncluded) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (isIncluded) Color(0xFF4CAF50) else RecRed.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feature,
                        fontSize = 12.sp,
                        fontWeight = if (isIncluded) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isIncluded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
