package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

@Composable
fun ProUpsellDialog(
    currentTier: UserTier = UserTier.FREE,
    onDismiss: () -> Unit,
    onSelectTier: (UserTier) -> Unit
) {
    var selectedTierTab by remember { mutableStateOf(if (currentTier == UserTier.FREE) UserTier.PRO else currentTier) }

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
                        contentDescription = "Tier Upgrade",
                        tint = AmberAccent,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Soundboard Membership", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "Choose the tier that fits your parenting & soundboard needs:",
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
                        badge = "$4.99/mo",
                        isSelected = selectedTierTab == UserTier.PRO,
                        onClick = { selectedTierTab = UserTier.PRO },
                        modifier = Modifier.weight(1f)
                    )

                    TierTabButton(
                        title = "ULTRA",
                        badge = "$9.99/mo",
                        isSelected = selectedTierTab == UserTier.ULTRA,
                        onClick = { selectedTierTab = UserTier.ULTRA },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Feature details card based on tab
                when (selectedTierTab) {
                    UserTier.FREE -> {
                        TierDetailCard(
                            tierName = "Standard (Free)",
                            description = "Essential local audio soundboard features for everyday quick taps.",
                            features = listOf(
                                "Up to 2 Soundboards" to true,
                                "Custom Audio Recording" to true,
                                "1 Home-Screen Widget" to true,
                                "Auto-Parent AI Listening" to false,
                                "Google Home Nest Broadcast" to false,
                                "Google Family Link Screen Sync" to false
                            ),
                            accentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    UserTier.PRO -> {
                        TierDetailCard(
                            tierName = "Soundboard PRO",
                            description = "Full AI local listening engine + unlimited audio customization.",
                            features = listOf(
                                "Unlimited Boards & Sound Clips" to true,
                                "⚡ Auto-Parent AI Listening Mode" to true,
                                "Audio Waveform Trimming & Edits" to true,
                                "Custom Acoustic Trigger Tags" to true,
                                "Google Home Nest Broadcast" to false,
                                "Google Family Link Screen Sync" to false
                            ),
                            accentColor = AmberAccent
                        )
                    }

                    UserTier.ULTRA -> {
                        TierDetailCard(
                            tierName = "Soundboard ULTRA Ecosystem",
                            description = "Complete smart home integration with Google Home & Family Link.",
                            features = listOf(
                                "Everything in PRO included" to true,
                                "📢 Google Home Nest Speaker Broadcast" to true,
                                "📱 Google Family Link Screen Time Pause" to true,
                                "💡 Smart Home Lighting Routines" to true,
                                "Multi-Device Family Cloud Sync" to true,
                                "Priority Gemini AI Processing" to true
                            ),
                            accentColor = AmberAccent
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSelectTier(selectedTierTab)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedTierTab) {
                        UserTier.FREE -> MaterialTheme.colorScheme.surfaceVariant
                        UserTier.PRO -> RecRed
                        UserTier.ULTRA -> AmberAccent
                    }
                )
            ) {
                Text(
                    text = when {
                        currentTier == selectedTierTab -> "Current Plan"
                        selectedTierTab == UserTier.FREE -> "Switch to Free"
                        selectedTierTab == UserTier.PRO -> "Unlock PRO"
                        else -> "Unlock ULTRA Ecosystem"
                    },
                    fontWeight = FontWeight.Bold,
                    color = if (selectedTierTab == UserTier.ULTRA) Color.Black else Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
