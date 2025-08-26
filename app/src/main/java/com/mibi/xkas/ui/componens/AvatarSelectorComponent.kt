package com.mibi.xkas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mibi.xkas.utils.AvatarUtils

@Composable
fun AvatarSelectorDialog(
    currentAvatarType: String,
    currentAvatarValue: String,
    currentAvatarColor: String,
    userName: String,
    onAvatarSelected: (type: String, value: String, color: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(if (currentAvatarType == "initial") 0 else 1) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Pilih Avatar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tab selector
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { selectedTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Inisial")
                    }

                    Button(
                        onClick = { selectedTab = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Preset")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content based on selected tab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    when (selectedTab) {
                        0 -> InitialAvatarSelector(
                            userName = userName,
                            currentColor = currentAvatarColor,
                            onColorSelected = { color ->
                                val initial = AvatarUtils.generateInitial(userName)
                                val colorHex = AvatarUtils.colorToHex(color)
                                onAvatarSelected("initial", initial, colorHex)
                            }
                        )
                        1 -> PresetAvatarSelector(
                            currentPresetId = if (currentAvatarType == "preset") currentAvatarValue else "",
                            onPresetSelected = { presetId ->
                                onAvatarSelected("preset", presetId, null)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onDismiss) {
                        Text("Selesai")
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialAvatarSelector(
    userName: String,
    currentColor: String,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFE53935),
        Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFF00ACC1),
        Color(0xFF3949AB), Color(0xFF8BC34A), Color(0xFFFF7043),
        Color(0xFFAB47BC)
    )

    val initial = AvatarUtils.generateInitial(userName)
    val selectedColor = if (currentColor.isNotEmpty()) {
        AvatarUtils.hexToColor(currentColor)
    } else {
        AvatarUtils.generateColorFromName(userName)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preview
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(selectedColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Color selector
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(80.dp)
        ) {
            items(colors) { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (color == selectedColor) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(color) }
                )
            }
        }
    }
}

@Composable
private fun PresetAvatarSelector(
    currentPresetId: String,
    onPresetSelected: (String) -> Unit
) {
    val presetAvatars = AvatarUtils.getAllPresetAvatarIds()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(presetAvatars) { avatarId ->
            val isSelected = avatarId == currentPresetId

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                    .clickable { onPresetSelected(avatarId) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = AvatarUtils.getPresetAvatarResource(avatarId)),
                    contentDescription = "Avatar $avatarId",
                    modifier = Modifier.size(36.dp),
                    tint = Color.Unspecified
                )
            }
        }
    }
}